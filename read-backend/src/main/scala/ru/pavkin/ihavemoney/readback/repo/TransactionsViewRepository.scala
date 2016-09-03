package ru.pavkin.ihavemoney.readback.repo

import java.time.OffsetDateTime
import java.util.UUID

import ru.pavkin.ihavemoney.domain.fortune.FortuneId
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.FortuneEvent

import scala.concurrent.{ExecutionContext, Future}

trait TransactionsViewRepository {

  def insert(evt: FortuneEvent): Future[Unit]

  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[List[FortuneEvent]]
  def findTransaction(transactionId: UUID)(implicit ec: ExecutionContext): Future[Option[FortuneEvent]]

  def removeOlderThan(date: OffsetDateTime)(implicit ec: ExecutionContext): Future[Unit]
}
