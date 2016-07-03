package ru.pavkin.ihavemoney.protocol

import io.circe._
import io.circe.generic.semiauto._
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency, FortuneInfo, Liability}

object readfront extends SharedProtocol {
  sealed trait FrontendQueryResult
  case class FrontendFortunes(userId: String, fortunes: List[FortuneInfo]) extends FrontendQueryResult
  case class FrontendCategories(fortuneId: String, income: List[String], expenses: List[String]) extends FrontendQueryResult
  case class FrontendMoneyBalance(fortuneId: String, balances: Map[Currency, BigDecimal]) extends FrontendQueryResult
  case class FrontendAssets(fortuneId: String, assets: Map[String, Asset]) extends FrontendQueryResult
  case class FrontendLiabilities(fortuneId: String, liabilities: Map[String, Liability]) extends FrontendQueryResult
  case class FrontendTransactions(fortuneId: String, transactions: List[Transaction]) extends FrontendQueryResult

  implicit val fqEncoder: Encoder[FrontendQueryResult] = deriveEncoder[FrontendQueryResult]
  implicit val fqDecoder: Decoder[FrontendQueryResult] = deriveDecoder[FrontendQueryResult]
}
