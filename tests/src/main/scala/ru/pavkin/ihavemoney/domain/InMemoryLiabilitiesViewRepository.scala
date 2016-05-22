package ru.pavkin.ihavemoney.domain

import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.readback.repo.{AssetsViewRepository, LiabilitiesViewRepository}

import scala.concurrent.{ExecutionContext, Future}

class InMemoryLiabilitiesViewRepository extends LiabilitiesViewRepository with InMemoryRepository[(LiabilityId, FortuneId), Liability] {
  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[LiabilityId, Liability]] = Future.successful {
    repo.filterKeys(_._1 == id)
      .map { case ((a, f), r) ⇒ a → r }
  }
}
