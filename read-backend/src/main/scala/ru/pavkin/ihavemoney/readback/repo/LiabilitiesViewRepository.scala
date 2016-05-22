package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.fortune._

import scala.concurrent.{ExecutionContext, Future}

trait LiabilitiesViewRepository extends Repository[(LiabilityId, FortuneId), Liability] {
  def findAll(id: FortuneId)(implicit ec: ExecutionContext): Future[Map[LiabilityId, Liability]]
}
