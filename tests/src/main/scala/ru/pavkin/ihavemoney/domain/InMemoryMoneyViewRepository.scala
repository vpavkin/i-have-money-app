package ru.pavkin.ihavemoney.domain

import ru.pavkin.ihavemoney.domain.fortune.{Currency, FortuneId}
import ru.pavkin.ihavemoney.readback.repo.MoneyViewRepository

import scala.concurrent.{ExecutionContext, Future}

class InMemoryMoneyViewRepository extends MoneyViewRepository {

  private var repo: Map[FortuneId, Map[Currency, BigDecimal]] = Map.empty

  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[Currency, BigDecimal]] = Future.successful {
    repo.getOrElse(id, Map.empty)
  }

  def byId(id: (FortuneId, Currency))(implicit ec: ExecutionContext): Future[Option[BigDecimal]] = Future.successful {
    repo.get(id._1).flatMap(_.get(id._2))
  }
  def updateById(id: (FortuneId, Currency), newAmount: BigDecimal)(implicit ec: ExecutionContext): Future[Unit] = Future.successful {
    repo = repo.updated(id._1, repo.getOrElse(id._1, Map.empty).updated(id._2, newAmount))
  }

  def insert(id: (FortuneId, Currency), amount: BigDecimal)(implicit ec: ExecutionContext): Future[Unit] =
    updateById(id, amount)
}
