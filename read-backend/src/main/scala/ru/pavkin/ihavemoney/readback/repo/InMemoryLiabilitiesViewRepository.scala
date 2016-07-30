package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.fortune._

import scala.concurrent.{ExecutionContext, Future}

class InMemoryLiabilitiesViewRepository extends LiabilitiesViewRepository with InMemoryRepository[(LiabilityId, FortuneId), Liability] {
  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[LiabilityId, Liability]] = Future.successful {
    repo.filterKeys(_._2 == id)
      .map { case ((a, f), r) ⇒ a → r }
  }
}
