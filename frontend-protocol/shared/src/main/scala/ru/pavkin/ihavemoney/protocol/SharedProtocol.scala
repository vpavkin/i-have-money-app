package ru.pavkin.ihavemoney.protocol

import java.time.LocalDate

import cats.data.Xor
import io.circe.syntax._
import io.circe._
import io.circe.generic.auto._
import ru.pavkin.ihavemoney.domain.fortune._

trait SharedProtocol {

  implicit val decodeCurrency: Decoder[Currency] =
    Decoder.decodeString.emap(s ⇒ Xor.fromOption(Currency.fromCode(s), s"$s is not a valid currency"))

  implicit val encodeCurrency: Encoder[Currency] =
    Encoder.instance(c ⇒ Json.fromString(c.code))

  implicit val assetEncoder = Encoder[Asset]
  implicit val assetDecoder = Decoder[Asset]

  implicit val liabilityEncoder = Encoder[Liability]
  implicit val liabilityDecoder = Decoder[Liability]

  implicit val currencyKeyEncoder: KeyEncoder[Currency] = KeyEncoder.instance(_.code)
  implicit val currencyKeyDecoder: KeyDecoder[Currency] = KeyDecoder.instance(Currency.fromCode)

  implicit val expenseCategoryKeyEncoder: KeyEncoder[ExpenseCategory] = KeyEncoder.instance(_.name)
  implicit val expenseCategoryKeyDecoder: KeyDecoder[ExpenseCategory] = KeyDecoder.instance(s ⇒ Some(ExpenseCategory(s)))

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
    Json.fromString(s"${p(time.getDayOfMonth)}-${p(time.getMonthValue)}-${time.getYear}")
  )

  private def p(s: Int) = "".padTo(2 - s.toString.length, "0").mkString + s.toString

  implicit val decoderEvent = Decoder[Event]
  implicit val encoderEvent = Encoder[Event]

  implicit val decoderWorth = Decoder[Worth]
  implicit val encoderWorth = Encoder[Worth]

  implicit val decoderExpenseCategory = Decoder[ExpenseCategory]
  implicit val encoderExpenseCategory = Encoder[ExpenseCategory]

  implicit val decoderFortuneInfo = Decoder[FortuneInfo]
  implicit val encoderFortuneInfo = Encoder[FortuneInfo]
}
