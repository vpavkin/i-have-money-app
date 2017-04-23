package ru.pavkin.ihavemoney.domain.query

import java.time.Year

import ru.pavkin.ihavemoney.domain.fortune.FortuneId
import ru.pavkin.ihavemoney.domain.user.UserId

case class QueryId(value: String)

sealed trait Query {
  val id: QueryId
  val user: UserId
}
sealed trait FortuneQuery extends Query {
  def fortuneId: FortuneId
}

case class MoneyBalance(id: QueryId, user: UserId, fortuneId: FortuneId) extends FortuneQuery
case class Assets(id: QueryId, user: UserId, fortuneId: FortuneId) extends FortuneQuery
case class Liabilities(id: QueryId, user: UserId, fortuneId: FortuneId) extends FortuneQuery
case class Categories(id: QueryId, user: UserId, fortuneId: FortuneId) extends FortuneQuery
case class TransactionLog(id: QueryId, user: UserId, fortuneId: FortuneId, year: Year) extends FortuneQuery
case class Fortunes(id: QueryId, user: UserId) extends Query
