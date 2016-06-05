package ru.pavkin.ihavemoney.protocol

import io.circe._
import io.circe.generic.semiauto._
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Liability}

object readfront extends SharedProtocol {
  sealed trait FrontendQueryResult
  case class FrontendFortunes(userId: String, fortunes: List[String]) extends FrontendQueryResult
  case class FrontendCategories(fortuneId: String, income: List[String], expenses: List[String]) extends FrontendQueryResult
  case class FrontendMoneyBalance(fortuneId: String, balances: Map[String, BigDecimal]) extends FrontendQueryResult
  case class FrontendAssets(fortuneId: String, balances: Map[String, Asset]) extends FrontendQueryResult
  case class FrontendLiabilities(fortuneId: String, balances: Map[String, Liability]) extends FrontendQueryResult

  implicit val fqEncoder: Encoder[FrontendQueryResult] = deriveEncoder[FrontendQueryResult]
  implicit val fqDecoder: Decoder[FrontendQueryResult] = deriveDecoder[FrontendQueryResult]
}
