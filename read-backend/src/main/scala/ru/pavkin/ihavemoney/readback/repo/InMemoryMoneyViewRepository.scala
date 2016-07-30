package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.fortune.{Currency, FortuneId}

import scala.concurrent.{ExecutionContext, Future}

class InMemoryMoneyViewRepository extends MoneyViewRepository with InMemoryRepository[(FortuneId, Currency), BigDecimal] {

  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[Currency, BigDecimal]] = Future.successful {
    repo.filterKeys(_._1 == id)
      .map { case ((_, c), r) ⇒ c → r }
  }
}
