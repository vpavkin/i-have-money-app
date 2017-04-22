package ru.pavkin.ihavemoney.readback.db

import java.util.UUID

import ru.pavkin.ihavemoney.domain.fortune.{AssetId, Currency, FortuneId, LiabilityId}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.GetResult
import slick.lifted.{Rep, TableQuery, Tag}

case class DebtRow(liabilityId: LiabilityId,
                   fortuneId: FortuneId,
                   name: String,
                   amount: BigDecimal,
                   currency: Currency,
                   interestRate: Option[BigDecimal])

class Debts(tableTag: Tag) extends Table[DebtRow](tableTag, "debts") {
  def * = (liabilityId, fortuneId, name, amount, currency, interestRate) <>(
    (t: (String, String, String, BigDecimal, String, Option[BigDecimal])) ⇒
      DebtRow(LiabilityId(UUID.fromString(t._1)), FortuneId(t._2), t._3, t._4, Currency.withName(t._5), t._6),
    (m: DebtRow) ⇒ Some((m.liabilityId.value.toString, m.fortuneId.value, m.name, m.amount, m.currency.code, m.interestRate))
    )

  val liabilityId: Rep[String] = column[String]("liability_id", O.PrimaryKey)
  val fortuneId: Rep[String] = column[String]("fortune_id")
  val name: Rep[String] = column[String]("name")
  val currency: Rep[String] = column[String]("currency")
  val amount: Rep[BigDecimal] = column[BigDecimal]("amount")
  val interestRate: Rep[Option[BigDecimal]] = column[Option[BigDecimal]]("interest_rate")
}

object Debts {

  implicit def GetResultDebtRow(implicit
                                e0: GetResult[String],
                                e1: GetResult[BigDecimal]): GetResult[DebtRow] = GetResult {
    prs ⇒ import prs._
      DebtRow(LiabilityId(UUID.fromString(<<[String])), FortuneId(<<[String]), <<[String], <<[BigDecimal], Currency.withName(<<[String]), <<[Option[BigDecimal]])
  }

  lazy val table = new TableQuery(tag ⇒ new Debts(tag))
}
