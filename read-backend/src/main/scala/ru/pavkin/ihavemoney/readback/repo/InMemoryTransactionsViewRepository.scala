package ru.pavkin.ihavemoney.readback.repo

import java.time.OffsetDateTime
import java.util.UUID

import ru.pavkin.ihavemoney.domain.fortune.FortuneId
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.FortuneEvent

import scala.concurrent.{ExecutionContext, Future}

class InMemoryTransactionsViewRepository extends TransactionsViewRepository {

  protected var repo: Map[FortuneId, List[FortuneEvent]] = Map.empty

  def insert(evt: FortuneEvent): Future[Unit] = Future.successful {
    repo += evt.metadata.aggregateId -> (repo.getOrElse(evt.metadata.aggregateId, Nil) :+ evt)
  }

  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[List[FortuneEvent]] =
    Future.successful(repo.getOrElse(id, Nil))

  def findTransaction(transactionId: UUID)(implicit ec: ExecutionContext): Future[Option[FortuneEvent]] =
    Future.successful(repo.flatMap(_._2).find(_.metadata.eventId.value == transactionId))

  def removeOlderThan(date: OffsetDateTime)(implicit ec: ExecutionContext): Future[Unit] =
    Future.successful(repo.mapValues(_.filterNot(_.metadata.date.isBefore(date))))

}
