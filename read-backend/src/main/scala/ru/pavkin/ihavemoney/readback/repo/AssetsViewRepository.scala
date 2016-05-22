package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.fortune.{Asset, AssetId, FortuneId}

import scala.concurrent.{ExecutionContext, Future}

trait AssetsViewRepository extends Repository[(AssetId, FortuneId), Asset] {
  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[List[Asset]]
}
