package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.{fortune ⇒ domain}
import domain.{Asset, AssetId, FortuneId}
import ru.pavkin.ihavemoney.readback.db._
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api.{Database, _}

import scala.concurrent.{ExecutionContext, Future}

class DatabaseAssetsViewRepository(db: Database) extends AssetsViewRepository {

  private def findQuery(id: AssetId) =
    Assets.table
        .filter(_.assetId === id.value.toString)

  private def toDomain(s: AssetRow) = domain.CountedAsset(s.name, s.price, s.currency, s.count)

  private def find(id: AssetId)(implicit ec: ExecutionContext): Future[Option[Asset]] = db.run {
    findQuery(id)
        .take(1)
        .result
        .map(_.headOption.map(toDomain))
  }

  def byId(id: (AssetId, FortuneId))(implicit ec: ExecutionContext): Future[Option[Asset]] = find(id._1)

  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[AssetId, Asset]] = db.run {
    Assets.table.filter(_.fortuneId === id.value).result.map(_.map(r ⇒ r.assetId → toDomain(r)))
  }.map(_.toMap)

  def replaceById(id: (AssetId, FortuneId), newRow: Asset)(implicit ec: ExecutionContext): Future[Unit] = newRow match {
    case s: domain.CountedAsset =>
      db.run {
        findQuery(id._1).update(AssetRow(id._1, id._2, s.name, s.price, s.currency, s.count))
      }.map(_ ⇒ ())
  }

  def insert(id: (AssetId, FortuneId), row: Asset)(implicit ec: ExecutionContext): Future[Unit] = row match {
    case s: domain.CountedAsset =>
      db.run {
        Assets.table += AssetRow(id._1, id._2, s.name, s.price, s.currency, s.count)
      }.map(_ ⇒ ())
  }

  def remove(id: (AssetId, FortuneId))(implicit ec: ExecutionContext): Future[Unit] = db.run {
    findQuery(id._1).delete
  }.map(_ ⇒ ())
}
