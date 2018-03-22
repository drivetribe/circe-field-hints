package com.drivetribe.circe.hinting

import cats.syntax.either._
import io.circe._
import io.circe.generic.decoding.DerivedDecoder
import shapeless.{Coproduct, Generic, HList, Poly2}
import shapeless.ops.coproduct.{Inject, ToHList}

import shapeless.ops.hlist.{LeftFolder, LiftAll}

trait HintedDecoder[T] extends Decoder[T] {
  def hints: Set[String]
}

object HintedDecoder {
  def apply[T](h: Set[String])(f: HCursor => Decoder.Result[T]) = new HintedDecoder[T] {
    val hints = h
    def apply(t: HCursor): Decoder.Result[T] = f(t)
  }

  def apply[T] = new Builder[T]

  class Builder[Type] {
    def upgrade(hint: String, decoder: Decoder[Type])(
      implicit configuration: HintingConfiguration
    ): HintedDecoder[Type] =
      HintedDecoder(Set(hint)) { t =>
        for {
          jsonHint <- t.downField(configuration.hintFieldName).as[String]
          _ <- if (jsonHint == hint) Right(()) else Left(DecodingFailure(s"Unknown type hint: $jsonHint", Nil))
          decoded <- decoder(t)
        } yield decoded
      }

    def derive(hint: String)(
      implicit decoder: DerivedDecoder[Type],
      configuration: HintingConfiguration,
      generic: Generic.Aux[Type, _ <: HList]
    ): HintedDecoder[Type] =
      upgrade(hint, decoder)

    def coproduct[CoproductType <: Coproduct, TypeList <: HList, Decoders <: HList](
      implicit isCoproduct: CoproductType =:= Type,
      toTypeList: ToHList.Aux[CoproductType, TypeList],
      decoders: LiftAll.Aux[HintedDecoder, TypeList, Decoders],
      decoderMapFolder: LeftFolder.Aux[
        Decoders,
        DecoderMap[CoproductType],
        CoproductDecoderMapFolder.type,
        DecoderMap[CoproductType]
      ],
      configuration: HintingConfiguration
    ): HintedDecoder[Type] = new HintedDecoder[Type] {
      val emptyDecodersMap: DecoderMap[CoproductType] = Map.empty

      val decodersMap = decoders.instances.foldLeft(emptyDecodersMap)(CoproductDecoderMapFolder)

      val hints = decodersMap.keySet

      def apply(t: HCursor): Decoder.Result[Type] =
        for {
          jsonHint <- t.downField(configuration.hintFieldName).as[String]
          decoder <- decodersMap.get(jsonHint).toRight(DecodingFailure(s"Unknown type hint: $jsonHint", Nil))
          decoded <- decoder(t)
        } yield isCoproduct(decoded)
    }

    def adt[Repr <: Coproduct, TypeList <: HList, Decoders <: HList](
      implicit generic: Generic.Aux[Type, Repr],
      toTypeList: ToHList.Aux[Repr, TypeList],
      decoders: LiftAll.Aux[HintedDecoder, TypeList, Decoders],
      decoderMapFolder: LeftFolder.Aux[Decoders, DecoderMap[Repr], CoproductDecoderMapFolder.type, DecoderMap[Repr]],
      configuration: HintingConfiguration
    ): HintedDecoder[Type] = {
      val coproductDecoder = HintedDecoder[Repr].coproduct
      HintedDecoder[Type](coproductDecoder.hints)(j => coproductDecoder(j).map(generic.from))
    }
  }

  type DecoderMap[T] = Map[String, Decoder[T]]

  object CoproductDecoderMapFolder extends Poly2 {
    implicit def forDecoderAndMap[Type <: Coproduct, Case](implicit inject: Inject[Type, Case]) =
      at[DecoderMap[Type], HintedDecoder[Case]] { (dm, hd) =>
        dm ++ hd.hints.map(_ -> hd.map(inject(_)))
      }
  }
}
