package ru.pavkin.ihavemoney.readback.db

import java.util.UUID

import ru.pavkin.ihavemoney.domain.fortune.{AssetId, Currency, FortuneId}
import slick.driver.PostgresDriver.api._
import slick.jdbc.GetResult
import slick.lifted.{Rep, TableQuery, Tag}

case class StocksRow(assetId: AssetId,
                     fortuneId: FortuneId,
                     name: String,
                     price: BigDecimal,
                     currency: Currency,
                     count: BigDecimal)

class Stocks(tableTag: Tag) extends Table[StocksRow](tableTag, "stocks") {
  def * = (assetId, fortuneId, name, price, currency, count) <>(
    (t: (String, String, String, BigDecimal, String, BigDecimal)) ⇒
      StocksRow(AssetId(UUID.fromString(t._1)), FortuneId(t._2), t._3, t._4, Currency.unsafeFromCode(t._5), t._6),
    (m: StocksRow) ⇒ Some((m.assetId.value.toString, m.fortuneId.value, m.name, m.price, m.currency.code, m.count))
    )

  val assetId: Rep[String] = column[String]("asset_id", O.PrimaryKey)
  val fortuneId: Rep[String] = column[String]("fortune_id")
  val name: Rep[String] = column[String]("name")
  val price: Rep[BigDecimal] = column[BigDecimal]("price")
  val currency: Rep[String] = column[String]("currency")
  val count: Rep[BigDecimal] = column[BigDecimal]("count")
}

object Stocks {

  implicit def GetResultStocksRow(implicit
                                  e0: GetResult[String],
                                  e1: GetResult[BigDecimal]): GetResult[StocksRow] = GetResult {
    prs ⇒ import prs._
      StocksRow(AssetId(UUID.fromString(<<[String])), FortuneId(<<[String]), <<[String], <<[BigDecimal], Currency.unsafeFromCode(<<[String]), <<[BigDecimal])
  }

  lazy val table = new TableQuery(tag ⇒ new Stocks(tag))
}
