package ru.pavkin.ihavemoney.readback.repo

import scala.concurrent.{ExecutionContext, Future}

trait Repository[PK, Row] {

  def byId(id: PK)(implicit ec: ExecutionContext): Future[Option[Row]]
  def replaceById(id: PK, newRow: Row)(implicit ec: ExecutionContext): Future[Unit]
  def insert(id: PK, row: Row)(implicit ec: ExecutionContext): Future[Unit]
  def remove(id: PK)(implicit ec: ExecutionContext): Future[Unit]

  def updateById(id: PK, updater: Row ⇒ Row)(implicit ec: ExecutionContext): Future[Unit] = for {
    row ← byId(id)
    _ ← row.foreach(r ⇒ replaceById(id, updater(r)))
  } yield ()
}
