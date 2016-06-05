package ru.pavkin.ihavemoney.domain.query

import ru.pavkin.ihavemoney.domain.fortune.FortuneId

case class QueryId(value: String)

sealed trait Query {
  val id: QueryId
}

case class MoneyBalance(id: QueryId, fortuneId: FortuneId) extends Query
case class Assets(id: QueryId, fortuneId: FortuneId) extends Query
case class Liabilities(id: QueryId, fortuneId: FortuneId) extends Query
case class Categories(id: QueryId, fortuneId: FortuneId) extends Query
