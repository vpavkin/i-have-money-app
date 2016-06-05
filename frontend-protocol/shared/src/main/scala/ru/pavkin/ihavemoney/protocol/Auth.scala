package ru.pavkin.ihavemoney.protocol

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._

case class Auth(email: String, displayName: String, token: String)

object Auth {
  implicit val decoderAuth = Decoder[Auth]
  implicit val encoderAuth = Encoder[Auth]
}
