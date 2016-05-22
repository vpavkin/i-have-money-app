package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.fortune.{RealEstate ⇒ _, Stocks ⇒ _, _}
import ru.pavkin.ihavemoney.domain.{fortune ⇒ domain}
import ru.pavkin.ihavemoney.readback.db._
import slick.driver.PostgresDriver.api.{Database, _}

import scala.concurrent.{ExecutionContext, Future}

class DatabaseLiabilitiesViewRepository(db: Database) extends LiabilitiesViewRepository {

  private def liabilitiesFindQuery(id: LiabilityId) =
    Debts.table
      .filter(_.liabilityId === id.value.toString)
      .take(1)

  private def liabilitiesRowToDomain(s: DebtRow) = s.interestRate match {
    case None ⇒ domain.NoInterestDebt(s.name, s.amount, s.currency)
    case Some(interest) ⇒ domain.Loan(s.name, s.amount, s.currency, interest)
  }

  private def find(id: LiabilityId)(implicit ec: ExecutionContext): Future[Option[Liability]] = db.run {
    liabilitiesFindQuery(id)
      .result
      .map(_.headOption.map(liabilitiesRowToDomain))
  }

  def byId(id: (LiabilityId, FortuneId))(implicit ec: ExecutionContext): Future[Option[Liability]] = find(id._1)

  def updateById(id: (LiabilityId, FortuneId), newRow: Liability)(implicit ec: ExecutionContext): Future[Unit] =
    db.run {
      liabilitiesFindQuery(id._1).update(DebtRow(id._1, id._2, newRow.name, newRow.amount, newRow.currency, newRow match {
        case n: NoInterestDebt => None
        case l: Loan => Some(l.interestRate)
      }))
    }.map(_ ⇒ ())

  def insert(id: (LiabilityId, FortuneId), row: Liability)(implicit ec: ExecutionContext): Future[Unit] =
    updateById(id, row)

}
