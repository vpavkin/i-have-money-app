package ru.pavkin.ihavemoney.readback.repo

import scala.concurrent.{ExecutionContext, Future}

trait Repository[PK, Row] {

  def byId(id: PK)(implicit ec: ExecutionContext): Future[Option[Row]]
  def updateById(id: PK, newRow: Row)(implicit ec: ExecutionContext): Future[Unit]
  def insert(id: PK, row: Row)(implicit ec: ExecutionContext): Future[Unit]
  def remove(id: PK)(implicit ec: ExecutionContext): Future[Unit]
}
