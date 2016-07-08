package ru.pavkin.ihavemoney.domain

import ru.pavkin.ihavemoney.domain.fortune.{Asset, AssetId, Currency, FortuneId}
import ru.pavkin.ihavemoney.readback.repo.{AssetsViewRepository, InMemoryRepository}

import scala.concurrent.{ExecutionContext, Future}

class InMemoryAssetsViewRepository extends AssetsViewRepository with InMemoryRepository[(AssetId, FortuneId), Asset] {

  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[AssetId, Asset]] = Future.successful {
    repo.filterKeys(_._2 == id)
      .map { case ((a, f), r) ⇒ a → r }
  }
}
