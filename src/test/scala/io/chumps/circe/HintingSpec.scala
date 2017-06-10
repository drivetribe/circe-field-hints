package io.chumps.circe.hinting

import io.circe.{Decoder, DecodingFailure, Json}
import io.circe.CursorOp.DownField
import io.circe.parser.decode
import io.circe.syntax._
import org.scalatest.{FunSpec, Matchers}
import shapeless.{:+:, CNil, Coproduct}

object HintingSpec {
  sealed trait Pet
  case class Cat(breed: String, weight: Double) extends Pet
  case class Dog(breed: String, weight: Double) extends Pet

  sealed trait Status
  case class On(battery: Double) extends Status
  case object Off extends Status

  sealed trait Toy
  case class Laser(battery: Status) extends Toy
  case class Ball(bouncy: Boolean) extends Toy

  val catHint = "pet.cat"
  val dogHint = "pet.dog"

  val pinky = Cat("Tabby", 6.5)
  val einstein = Dog("Saint Bernard", 25.7)

  def foobarJson(hint: String) =
    s"""{ "_type": "$hint", "breed": "foobar", "weight": 123.45 }"""

  val capibaraJson = foobarJson("pet.capibara")
  val capibaraError = Left(DecodingFailure("Unknown type hint: pet.capibara", Nil))

  val noHintJson = """{ "name": "Foo", "weight": 7 }"""
  val noHintError = Left(
    DecodingFailure(
      "Attempt to decode value on failed cursor",
      List(DownField(HintingConfiguration.default.hintFieldName))
    )
  )

  val offJson = """{"_type":"status.off"}"""

  implicit val hintingConfiguration = HintingConfiguration.default

  implicit val (onEncoder, onDecoder) = HintedCodec[On].derive("status.on")
  implicit val (offEncoder, offDecoder) = HintedCodec[Off.type].derive("status.off")
  implicit val (statusEncoder, statusDecoder) = HintedCodec[Status].adt

  implicit val (catEncoder, catDecoder) = HintedCodec[Cat].derive(catHint)
  implicit val (dogEncoder, dogDecoder) = HintedCodec[Dog].derive(dogHint)

  implicit val (petDecoder, petEncoder) = HintedCodec[Pet].adt
}

class HintingSpec extends FunSpec with Matchers {
  import HintingSpec._

  def getHint(json: Json): Decoder.Result[String] = json.hcursor.downField("_type").as[String]

  describe("Derived hinted codecs") {
    it("introduce type hints when encoding case classes") {
      getHint(pinky.asJson) shouldEqual Right(catHint)
      getHint(einstein.asJson) shouldEqual Right(dogHint)
    }

    it("support type hints when decoding a case class") {
      val Seq(catJson, dogJson) = Seq(catHint, dogHint).map(foobarJson)

      decode[Cat](catJson) shouldEqual (Right(Cat("foobar", 123.45)))
      decode[Dog](dogJson) shouldEqual (Right(Dog("foobar", 123.45)))
    }

    it("encode case objects as an objects with only the hint field") {
      Off.asJson.noSpaces shouldEqual (offJson)
    }

    it("decode a case object from an object with only the hint field") {
      decode[Status](offJson) shouldEqual (Right(Off))
      decode[Off.type](offJson) shouldEqual (Right(Off))
    }

    it("raise an error if a type hint is invalid when decoding a case class") {
      decode[Cat](capibaraJson) shouldEqual (capibaraError)
      decode[Dog](capibaraJson) shouldEqual (capibaraError)
    }

    it("raise an error if a type hint is missing when decoding a case class") {
      decode[Cat](noHintJson) shouldEqual (noHintError)
      decode[Dog](noHintJson) shouldEqual (noHintError)
    }

    it("fail compiling when trying to define derive codecs for coproduct types") {
      """HintedDecoder[Toy].derive("toy")""" shouldNot typeCheck
      """HintedEncoder[Toy].derive("toy")""" shouldNot typeCheck
      """HintedCodec[Toy].derive("toy")""" shouldNot typeCheck
    }
  }

  describe("ADT hinted codecs") {
    it ("introduce type hints when encoding a hinted sealed trait") {
      getHint(pinky.asInstanceOf[Pet].asJson) shouldEqual Right(catHint)
      getHint(einstein.asInstanceOf[Pet].asJson) shouldEqual Right(dogHint)
    }

    it("support type hints when decoding a sealed trait") {
      val Seq(catJson, dogJson) = Seq(catHint, dogHint).map(foobarJson)

      decode[Pet](catJson) shouldEqual (Right(Cat("foobar", 123.45)))
      decode[Pet](dogJson) shouldEqual (Right(Dog("foobar", 123.45)))
    }

    it("fail compiling when trying to define codecs for a sealed trait whose children are not defined") {
      implicit val (ballEncoder, ballDecoder) = HintedCodec[Ball].derive("toy.ball")

      """HintedEncoder[Toy].adt""" shouldNot typeCheck
      """HintedDecoder[Toy].adt""" shouldNot typeCheck
      """HintedCodec[Toy].adt""" shouldNot typeCheck
    }

    it("raise an error if a type hint is invalid when decoding a case class") {
      decode[Pet](capibaraJson) shouldEqual (capibaraError)
    }

    it("raise an error if a type hint is missing when decoding a case class") {
      decode[Pet](noHintJson) shouldEqual (noHintError)
    }
  }

  describe("Coproduct hinted codecs") {
    case object House
    case class Human(name: String)
    type HHPT = House.type :+: Human :+: Pet :+: Toy :+: CNil

    implicit val (houseEncoder, houseDecoder) = HintedCodec[House.type].derive("house")
    implicit val (humanEncoder, humanDecoder) = HintedCodec[Human].derive("human")
    implicit val (laserEncoder, laserDecoder) = HintedCodec[Laser].derive("toy.laser")
    implicit val (ballEncoder, ballDecoder) = HintedCodec[Ball].derive("toy.ball")
    implicit val (toyEncoder, toyDecoder) = HintedCodec[Toy].adt
    implicit val (petOrToyEncoder, petOrToyDecoder) = HintedCodec[HHPT].coproduct

    val kilgore = Human("Kilgore Trout")
    val laser = Laser(Off)
    val ball = Ball(true)

    it("allow composing sealed codecs in coproduct codecs") {
      decode[HHPT](laser.asJson.noSpaces) shouldEqual(Right(Coproduct[HHPT](laser: Toy)))
      decode[HHPT](ball.asJson.noSpaces) shouldEqual(Right(Coproduct[HHPT](ball: Toy)))
      decode[HHPT](pinky.asJson.noSpaces) shouldEqual(Right(Coproduct[HHPT](pinky: Pet)))
      decode[HHPT](einstein.asJson.noSpaces) shouldEqual(Right(Coproduct[HHPT](einstein: Pet)))
      decode[HHPT](kilgore.asJson.noSpaces) shouldEqual(Right(Coproduct[HHPT](kilgore)))
      decode[HHPT](House.asJson.noSpaces) shouldEqual(Right(Coproduct[HHPT](House)))
    }

    it("raise an error if a type hint is invalid when decoding a case class") {
      decode[HHPT](capibaraJson) shouldEqual (capibaraError)
    }

    it("raise an error if a type hint is missing when decoding a case class") {
      decode[HHPT](noHintJson) shouldEqual (noHintError)
    }
  }
}
