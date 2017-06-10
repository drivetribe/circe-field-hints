package io.chumps.circe.hinting

import io.circe.{Encoder, Json}
import io.circe.generic.encoding.DerivedObjectEncoder

import shapeless.{Coproduct, Generic, HList, Poly1}
import shapeless.ops.coproduct.{Folder}

trait HintedEncoder[T] extends Encoder[T]

object HintedEncoder {
  private def apply[T](f: T => Json) = new HintedEncoder[T] {
    def apply(t: T): Json = f(t)
  }

  def apply[T] = new Builder[T]

  class Builder[Type] {
    def upgrade(hint: String, encoder: Encoder[Type])(
      implicit configuration: HintingConfiguration
    ): HintedEncoder[Type] =
      HintedEncoder(t => encoder(t).deepMerge(Json.obj(configuration.hintFieldName -> Json.fromString(hint))))

    def derive(hint: String)(
      implicit encoder: DerivedObjectEncoder[Type],
      configuration: HintingConfiguration,
      generic: Generic.Aux[Type, _ <: HList]
    ): HintedEncoder[Type] =
      upgrade(hint, encoder)

    def coproduct[CoproductType <: Coproduct](
      implicit isCoproduct: Type =:= CoproductType,
      canApply: Folder.Aux[ToHintedJson.type, CoproductType, Json]
    ): HintedEncoder[Type] =
      HintedEncoder((t: Type) => isCoproduct(t).fold(ToHintedJson))

    def adt[Repr <: Coproduct](
      implicit generic: Generic.Aux[Type, Repr],
      canApply: Folder.Aux[ToHintedJson.type, Repr, Json]
    ): HintedEncoder[Type] =
      HintedEncoder((t: Type) => HintedEncoder[Repr].coproduct.apply(generic.to(t)))
  }

  object ToHintedJson extends Poly1 {
    implicit def forHintedEncoder[T](implicit he: HintedEncoder[T]) = at[T](he.apply(_))
  }
}
