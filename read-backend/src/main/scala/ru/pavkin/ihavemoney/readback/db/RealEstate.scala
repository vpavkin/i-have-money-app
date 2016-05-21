package ru.pavkin.ihavemoney.readback.db

import java.util.UUID

import ru.pavkin.ihavemoney.domain.fortune.{AssetId, Currency, FortuneId}
import slick.driver.PostgresDriver.api._
import slick.jdbc.GetResult
import slick.lifted.{Rep, TableQuery, Tag}

case class RealEstateRow(assetId: AssetId,
                         fortuneId: FortuneId,
                         name: String,
                         price: BigDecimal,
                         currency: Currency)

class RealEstate(tableTag: Tag) extends Table[RealEstateRow](tableTag, "realestate") {
  def * = (assetId, fortuneId, name, price, currency) <>(
    (t: (String, String, String, BigDecimal, String)) ⇒
      RealEstateRow(AssetId(UUID.fromString(t._1)), FortuneId(t._2), t._3, t._4, Currency.unsafeFromCode(t._5)),
    (m: RealEstateRow) ⇒ Some((m.assetId.value.toString, m.fortuneId.value, m.name, m.price, m.currency.code))
    )

  val assetId: Rep[String] = column[String]("asset_id", O.PrimaryKey)
  val fortuneId: Rep[String] = column[String]("fortune_id")
  val name: Rep[String] = column[String]("name")
  val price: Rep[BigDecimal] = column[BigDecimal]("price")
  val currency: Rep[String] = column[String]("currency")
}

object RealEstate {

  implicit def GetResultRealEstateRow(implicit
                                      e0: GetResult[String],
                                      e1: GetResult[BigDecimal]): GetResult[RealEstateRow] = GetResult {
    prs ⇒ import prs._
      RealEstateRow(AssetId(UUID.fromString(<<[String])), FortuneId(<<[String]), <<[String], <<[BigDecimal], Currency.unsafeFromCode(<<[String]))
  }

  lazy val table = new TableQuery(tag ⇒ new RealEstate(tag))
}
