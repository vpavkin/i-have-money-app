package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.fortune.{Currency, FortuneId}

import scala.concurrent.{ExecutionContext, Future}

trait MoneyViewRepository extends Repository[(FortuneId, Currency), BigDecimal] {
  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[Currency, BigDecimal]]
}
