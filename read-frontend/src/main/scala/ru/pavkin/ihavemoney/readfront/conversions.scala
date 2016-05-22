package ru.pavkin.ihavemoney.readfront

import ru.pavkin.ihavemoney.domain.query._
import ru.pavkin.ihavemoney.protocol.readfront._

object conversions {

  def toFrontendFormat(qr: QueryResult): FrontendQueryResult = qr match {
    case MoneyBalanceQueryResult(id, balance) ⇒
      FrontendMoneyBalance(id.value, balance.map(kv ⇒ kv._1.code → kv._2))
    case EntityNotFound(id, error) ⇒
      FrontendEntityNotFound(id.value, error)
    case QueryFailed(id, error) ⇒
      FrontendQueryFailed(id.value, error)
    case LiabilitiesQueryResult(id, liabilities) =>
      FrontendLiabilities(id.value, liabilities)
    case AssetsQueryResult(id, assets) =>
      FrontendAssets(id.value, assets)
  }
}
