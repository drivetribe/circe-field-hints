package io.chumps.circe.hinting

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder
import shapeless.{Coproduct, Generic, HList}
import shapeless.ops.coproduct.{Folder, ToHList}
import shapeless.ops.hlist.{LeftFolder, LiftAll}

object HintedCodec {
  def apply[T] = new Builder[T]

  class Builder[Type] {
    def upgrade(hint: String, encoder: Encoder[Type], decoder: Decoder[Type])(
      implicit configuration: HintingConfiguration
    ) =
      (HintedEncoder[Type].upgrade(hint, encoder), HintedDecoder[Type].upgrade(hint, decoder))

    def derive(hint: String)(
      implicit encoder: DerivedObjectEncoder[Type],
      decoder: DerivedDecoder[Type],
      configuration: HintingConfiguration,
      generic: Generic.Aux[Type, _ <: HList]
    ) =
      upgrade(hint, encoder, decoder)

    def adt[Repr <: Coproduct, TypeList <: HList, Decoders <: HList](
      implicit configuration: HintingConfiguration,
      generic: Generic.Aux[Type, Repr],
      canEncode: Folder.Aux[HintedEncoder.ToHintedJson.type, Repr, Json],
      hintFor: Folder.Aux[HintedEncoder.HintFor.type, Repr, String],
      toHList: ToHList.Aux[Repr, TypeList],
      decoders: LiftAll.Aux[HintedDecoder, TypeList, Decoders],
      decoderMapFolder: LeftFolder.Aux[
        Decoders,
        HintedDecoder.DecoderMap[Repr],
        HintedDecoder.CoproductDecoderMapFolder.type,
        HintedDecoder.DecoderMap[Repr]
      ]
    ) =
      (HintedEncoder[Type].adt, HintedDecoder[Type].adt)

    def coproduct[Repr <: Coproduct, TypeList <: HList, Decoders <: HList](
      implicit configuration: HintingConfiguration,
      isCoproduct: Repr =:= Type,
      canEncode: Folder.Aux[HintedEncoder.ToHintedJson.type, Repr, Json],
      hintFor: Folder.Aux[HintedEncoder.HintFor.type, Repr, String],
      toHList: ToHList.Aux[Repr, TypeList],
      decoders: LiftAll.Aux[HintedDecoder, TypeList, Decoders],
      decoderMapFolder: LeftFolder.Aux[
        Decoders,
        HintedDecoder.DecoderMap[Repr],
        HintedDecoder.CoproductDecoderMapFolder.type,
        HintedDecoder.DecoderMap[Repr]
      ]
    ) =
      (HintedEncoder[Repr].coproduct, HintedDecoder[Repr].coproduct)
  }
}
