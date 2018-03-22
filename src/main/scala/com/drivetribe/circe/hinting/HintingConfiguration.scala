package com.drivetribe.circe.hinting

case class HintingConfiguration(hintFieldName: String)

object HintingConfiguration {
  val default = HintingConfiguration("_type")
}
