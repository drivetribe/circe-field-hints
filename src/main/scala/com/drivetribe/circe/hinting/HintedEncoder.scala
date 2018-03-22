package com.drivetribe.circe.hinting

import io.circe.{Encoder, Json}
import io.circe.generic.encoding.DerivedObjectEncoder

import shapeless.{Coproduct, Generic, HList, Poly1}
import shapeless.ops.coproduct.{Folder}

trait HintedEncoder[T] extends Encoder[T] {
  def hintFor(t: T): String
}

object HintedEncoder {
  private def apply[T](f: T => Json, h: T => String) = new HintedEncoder[T] {
    def apply(t: T): Json = f(t)
    def hintFor(t: T): String = h(t)
  }

  def apply[T] = new Builder[T]

  def hintFor[T](t: T)(implicit e: HintedEncoder[T]): String = e.hintFor(t)

  class Builder[Type] {
    def upgrade(hint: String, encoder: Encoder[Type])(
      implicit configuration: HintingConfiguration
    ): HintedEncoder[Type] =
      HintedEncoder(
        t => encoder(t).deepMerge(Json.obj(configuration.hintFieldName -> Json.fromString(hint))),
        _ => hint
      )

    def derive(hint: String)(
      implicit encoder: DerivedObjectEncoder[Type],
      configuration: HintingConfiguration,
      generic: Generic.Aux[Type, _ <: HList]
    ): HintedEncoder[Type] =
      upgrade(hint, encoder)

    def coproduct[CoproductType <: Coproduct](
      implicit isCoproduct: Type =:= CoproductType,
      toHintedJson: Folder.Aux[ToHintedJson.type, CoproductType, Json],
      hintFor: Folder.Aux[HintFor.type, CoproductType, String]
    ): HintedEncoder[Type] =
      HintedEncoder(
        (t: Type) => isCoproduct(t).fold(ToHintedJson),
        (t: Type) => isCoproduct(t).fold(HintFor)
      )

    def adt[Repr <: Coproduct](
      implicit generic: Generic.Aux[Type, Repr],
      canApply: Folder.Aux[ToHintedJson.type, Repr, Json],
      hintFor: Folder.Aux[HintFor.type, Repr, String]
    ): HintedEncoder[Type] = {
      val coproductEncoder = HintedEncoder[Repr].coproduct
      HintedEncoder(
        (t: Type) => coproductEncoder.apply(generic.to(t)),
        (t: Type) => coproductEncoder.hintFor(generic.to(t))
      )
    }
  }

  object HintFor extends Poly1 {
    implicit def forHintedEncoder[T](implicit he: HintedEncoder[T]) = at[T](he.hintFor(_))
  }

  object ToHintedJson extends Poly1 {
    implicit def forHintedEncoder[T](implicit he: HintedEncoder[T]) = at[T](he.apply(_))
  }
}
