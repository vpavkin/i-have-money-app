package ru.pavkin.ihavemoney.readback.db

import java.util.UUID

import ru.pavkin.ihavemoney.domain.fortune.{AssetId, Currency, FortuneId}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.GetResult
import slick.lifted.{Rep, TableQuery, Tag}

case class AssetRow(
    assetId: AssetId,
    fortuneId: FortuneId,
    name: String,
    price: BigDecimal,
    currency: Currency,
    count: BigDecimal)

class Assets(tableTag: Tag) extends Table[AssetRow](tableTag, "assets") {
  def * = (assetId, fortuneId, name, price, currency, count) <>(
      (t: (String, String, String, BigDecimal, String, BigDecimal)) ⇒
        AssetRow(AssetId(UUID.fromString(t._1)), FortuneId(t._2), t._3, t._4, Currency.unsafeFromCode(t._5), t._6),
      (m: AssetRow) ⇒ Some((m.assetId.value.toString, m.fortuneId.value, m.name, m.price, m.currency.code, m.count))
      )

  val assetId: Rep[String] = column[String]("asset_id", O.PrimaryKey)
  val fortuneId: Rep[String] = column[String]("fortune_id")
  val name: Rep[String] = column[String]("name")
  val price: Rep[BigDecimal] = column[BigDecimal]("price")
  val currency: Rep[String] = column[String]("currency")
  val count: Rep[BigDecimal] = column[BigDecimal]("count")
}

object Assets {

  implicit def GetResultAssetsRow(
      implicit
      e0: GetResult[String],
      e1: GetResult[BigDecimal]): GetResult[AssetRow] = GetResult {
    prs ⇒ import prs._
      AssetRow(AssetId(UUID.fromString(<<[String])), FortuneId(<<[String]), <<[String], <<[BigDecimal], Currency.unsafeFromCode(<<[String]), <<[BigDecimal])
  }

  lazy val table = new TableQuery(tag ⇒ new Assets(tag))
}
