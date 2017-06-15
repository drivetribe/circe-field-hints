# Circe field hints

## Rationale

This module provides a configurable alternative to circe built-in type hinting strategy. As an example given the following ADT and data:

    sealed trait Pet
    case class Cat(breed: String, weight: Double) extends Pet
    case class Dog(breed: String, weight: Double) extends Pet

    val pinkyCat = Cat("Tabby", 6.5)
    val pinkyPet: Pet = pinkyCat

The built-in circe strategy to deriving a codec for `Pet` would work as follows:

    import io.circe.Encoder
    import io.circe.generic.semiauto._
    import io.circe.syntax._

    implicit val catEncoder: Encoder[Cat] = deriveEncoder
    implicit val dogEncoder: Encoder[Dog] = deriveEncoder
    implicit val petEncoder: Encoder[Pet] = deriveEncoder

    println(pinkyCat.asJson)
    // Relies on `catEncoder`, produces: { "breed": "Tabby", "weight": 6.5 }

    println(pinkyPet.asJson)
    // Relies on `petEncoder`, Produces: { "Cat": { "breed": "Tabby", "weight": 6.5 } }

Using the `adt` function from this library instead it is possible to do the following:

    import io.chumps.circe.hinting._
    import io.circe.syntax._

    implicit val hintingConfiguration = HintingConfiguration.default
    implicit val catEncoder = HintedEncoder[Cat].derive("pet.cat")
    implicit val dogEncoder = HintedEncoder[Dog].derive("pet.dog")
    implicit val petEncoder = HintedEncoder[Pet].adt

    println(pinkyCat.asJson)
    // Relies on `catEncoder`, produces: { "_type": "pet.cat", "breed": "Tabby", "weight": 6.5 }

    println(pinkyPet.asJson)
    // Relies on `petEncoder`, Produces: { "_type": "pet.cat", "breed": "Tabby", "weight": 6.5 }

## Installation

`circe-field-hints` is available on Bintray. This is the dependency for sbt:

    "io.chumps" %% "circe-field-hints" % "0.1"

## The API

The library relies on finding an implicit `HintingConfiguration` in scope to decide which field to use for type hinting. As you can see in the previous example the dafault is `_type`.

The `HintedCodec` module is an utility that exposes the same API and allows the creation of encoder/decoder pairs as in:

    implicit val (catEncoder, catDecoder) = HintedCodec[Cat].derive("pet.cat")

The `HintedDecoder`, `HintedEncoder` and `HintedCodec` modules provide the same API which exposes the following functions:

* `upgrade` - upgrades an existing codec with type hints
* `derive` - uses circe semiauto derivation to create an hinted codec for a product type (e.g. case classes)
* `adt` - defines a codec for a sealed hierarchy whose cases each have an implicitly available hinted codec; if any is missing it won't compile.
* `coproduct` - defines a codec for a shapeless `Coproduct` whose types each have an implicitly available hinted codec; if any is missing it won't compile.

For more examples of usage of these functions please refer to `HintingSpec`.
