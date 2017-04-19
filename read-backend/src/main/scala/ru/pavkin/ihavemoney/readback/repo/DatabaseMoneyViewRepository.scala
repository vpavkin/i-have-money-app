package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.fortune.{Currency, FortuneId}
import ru.pavkin.ihavemoney.readback.db.{Money, MoneyRow}
import slick.jdbc.PostgresProfile.api.{Database, _}

import scala.concurrent.{ExecutionContext, Future}

class DatabaseMoneyViewRepository(db: Database) extends MoneyViewRepository {

  private def findQuery(id: FortuneId, currency: Currency): Query[Money, MoneyRow, Seq] =
    Money.table
      .filter(money ⇒ money.fortuneId === id.value && money.currency === currency.code)

  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[Currency, BigDecimal]] = db.run {
    Money.table.filter(_.fortuneId === id.value).result
  }.map(_.map(a ⇒ a.currency → a.amount).toMap)

  def byId(id: (FortuneId, Currency))(implicit ec: ExecutionContext): Future[Option[BigDecimal]] = db.run {
    findQuery(id._1, id._2)
      .take(1)
      .map(_.amount)
      .result
  }.map(_.headOption)

  def replaceById(id: (FortuneId, Currency), newRow: BigDecimal)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    findQuery(id._1, id._2).map(_.amount).update(newRow)
  }.map(_ ⇒ ())

  def insert(id: (FortuneId, Currency), row: BigDecimal)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    Money.table += MoneyRow(id._1, id._2, row)
  }.map(_ ⇒ ())

  def remove(id: (FortuneId, Currency))(implicit ec: ExecutionContext): Future[Unit] = db.run {
    findQuery(id._1, id._2).delete
  }.map(_ ⇒ ())
}
