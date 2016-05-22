package ru.pavkin.ihavemoney.domain

import ru.pavkin.ihavemoney.readback.repo.Repository

import scala.concurrent.{ExecutionContext, Future}

trait InMemoryRepository[PK, Row] extends Repository[PK, Row] {

  protected var repo: Map[PK, Row] = Map.empty

  def byId(id: PK)(implicit ec: ExecutionContext): Future[Option[Row]] = Future.successful(repo.get(id))

  def replaceById(id: PK, newRow: Row)(implicit ec: ExecutionContext): Future[Unit] = Future.successful {
    repo = repo.updated(id, newRow)
  }

  def insert(id: PK, row: Row)(implicit ec: ExecutionContext): Future[Unit] =
    replaceById(id, row)

  def remove(id: PK)(implicit ec: ExecutionContext): Future[Unit] = Future {
    repo = repo - id
  }
}
