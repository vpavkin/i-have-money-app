package ru.pavkin.ihavemoney.protocol

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._

case class FailedRequest(requestId: String, errorMessage: String)

object FailedRequest {
  implicit val failedRequestEncoder = Encoder[FailedRequest]
  implicit val failedRequestDecoder = Decoder[FailedRequest]
}
