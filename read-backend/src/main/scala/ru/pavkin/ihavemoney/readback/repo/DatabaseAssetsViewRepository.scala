package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.{fortune ⇒ domain}
import domain.{Asset, AssetId, FortuneId}
import ru.pavkin.ihavemoney.readback.db._
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api.{Database, _}

import scala.concurrent.{ExecutionContext, Future}

class DatabaseAssetsViewRepository(db: Database) extends AssetsViewRepository {

  private def stocksFindQuery(id: AssetId) =
    Stocks.table
      .filter(_.assetId === id.value.toString)
      .take(1)

  private def realEstateFindQuery(id: AssetId) =
    RealEstate.table
      .filter(_.assetId === id.value.toString)
      .take(1)

  private def stocksRowToDomain(s: StocksRow) = domain.Stocks(s.name, s.price, s.currency, s.count)
  private def realEstateRowToDomain(s: RealEstateRow) = domain.RealEstate(s.name, s.price, s.currency)

  private def find(id: AssetId)(implicit ec: ExecutionContext): Future[Option[Asset]] = db.run {
    stocksFindQuery(id)
      .result
      .map(_.headOption.map(stocksRowToDomain))
  }.flatMap {
    case None ⇒ db.run {
      realEstateFindQuery(id)
        .result
        .map(_.headOption.map(realEstateRowToDomain))
    }
    case Some(s) ⇒ Future.successful(Some(s))
  }

  def byId(id: (AssetId, FortuneId))(implicit ec: ExecutionContext): Future[Option[Asset]] = find(id._1)

  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[AssetId, Asset]] = {
    val r1 = db.run {
      RealEstate.table.filter(_.fortuneId === id.value).result.map(_.map(r ⇒ r.assetId → realEstateRowToDomain(r)))
    }
    val r2 = db.run {
      Stocks.table.filter(_.fortuneId === id.value).result.map(_.map(r ⇒ r.assetId → stocksRowToDomain(r)))
    }
    r1.flatMap(s1 ⇒ r2.map(_ ++ s1)).map(_.toMap)
  }

  def replaceById(id: (AssetId, FortuneId), newRow: Asset)(implicit ec: ExecutionContext): Future[Unit] = newRow match {
    case s: domain.Stocks =>
      db.run {
        stocksFindQuery(id._1).update(StocksRow(id._1, id._2, s.name, s.price, s.currency, s.count))
      }.map(_ ⇒ ())
    case r: domain.RealEstate =>
      db.run {
        realEstateFindQuery(id._1).update(RealEstateRow(id._1, id._2, r.name, r.price, r.currency))
      }.map(_ ⇒ ())

  }

  def insert(id: (AssetId, FortuneId), row: Asset)(implicit ec: ExecutionContext): Future[Unit] = replaceById(id, row)

  def remove(id: (AssetId, FortuneId))(implicit ec: ExecutionContext): Future[Unit] = db.run {
    for {
      _ ← stocksFindQuery(id._1).delete
      r ← realEstateFindQuery(id._1).delete
    } yield r
  }.map(_ ⇒ ())
}
