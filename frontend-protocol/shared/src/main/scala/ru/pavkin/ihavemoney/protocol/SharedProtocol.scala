package ru.pavkin.ihavemoney.protocol

import java.time.LocalDate

import cats.data.Xor
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
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

  implicit def currencyMapEncoder[T: Encoder]: Encoder[Map[Currency, T]] = Encoder.instance(_.map { case (k, v) ⇒ k.code -> v }.asJson)

  implicit def currencyMapDecoder[T: Decoder]: Decoder[Map[Currency, T]] = Decoder.instance { c ⇒
    Decoder.decodeMap[Map, T].apply(c)
      .flatMap(_.foldLeft[Decoder.Result[Map[Currency, T]]](Xor.right(Map.empty[Currency, T])) {
        case (m, (k, v)) ⇒ m.flatMap(mm ⇒
          Currency.fromCode(k) match {
            case Some(c) ⇒ Xor.right(mm + (c → v))
            case None ⇒ Xor.left(DecodingFailure(s"$k is not a valid currency", Nil))
          }
        )
      })
  }


  implicit final val decodeLocalDateDefault: Decoder[LocalDate] = Decoder.instance { c =>
    c.as[String].flatMap { s =>
      s.split("-").toList match {
        case day :: month :: year :: Nil ⇒
          Xor.catchNonFatal(LocalDate.of(year.toInt, month.toInt, day.toInt))
            .leftMap(ex ⇒ DecodingFailure(ex.getMessage, c.history))
        case _ ⇒ Xor.left(DecodingFailure("Invalid date string", c.history))
      }
    }
  }

  implicit final val encodeLocalDateDefault: Encoder[LocalDate] = Encoder.instance(time =>
    Json.string(s"${p(time.getDayOfMonth)}:${p(time.getMonthValue)}:${time.getYear}")
  )

  private def p(s: Int) = "".padTo(2, "0").mkString + s.toString
  implicit val decoderTransaction = Decoder[Transaction]
  implicit val encoderTransaction = Encoder[Transaction]
}
