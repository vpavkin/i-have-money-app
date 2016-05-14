package ru.pavkin.ihavemoney.protocol

import cats.data.Xor
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency, Liability}

trait SharedProtocol {

  implicit val decodeCurrency: Decoder[Currency] =
    Decoder.decodeString.emap(s ⇒ Currency.fromCode(s) match {
      case Some(c) ⇒ Xor.Right(c)
      case None ⇒ Xor.Left(s"$s is not a valid currency")
    })

  implicit val encodeCurrency: Encoder[Currency] =
    Encoder.instance(c ⇒ Json.string(c.code))

  implicit val assetEncoder = Encoder[Asset]
  implicit val assetDecoder = Decoder[Asset]

  implicit val liabilityEncoder = Encoder[Liability]
  implicit val liabilityDecoder = Decoder[Liability]
}
