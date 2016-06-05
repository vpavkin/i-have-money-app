package ru.pavkin.ihavemoney.domain.query

import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.{ExpenseCategory, IncomeCategory}
import ru.pavkin.ihavemoney.domain.fortune._

sealed trait QueryResult

case class CategoriesQueryResult(id: FortuneId, income: List[IncomeCategory], expences: List[ExpenseCategory]) extends QueryResult
case class MoneyBalanceQueryResult(id: FortuneId, balance: Map[Currency, BigDecimal]) extends QueryResult
case class AssetsQueryResult(id: FortuneId, assets: Map[String, Asset]) extends QueryResult
case class LiabilitiesQueryResult(id: FortuneId, liabilities: Map[String, Liability]) extends QueryResult

case class EntityNotFound(id: QueryId, error: String) extends QueryResult
case class QueryFailed(id: QueryId, error: String) extends QueryResult
