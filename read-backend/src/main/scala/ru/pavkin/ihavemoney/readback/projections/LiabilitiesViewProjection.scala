package ru.pavkin.ihavemoney.readback.projections

import io.funcqrs.{HandleEvent, Projection}
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.readback.repo.{AssetsViewRepository, LiabilitiesViewRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LiabilitiesViewProjection(liabilitiesRepo: LiabilitiesViewRepository) extends Projection {

  def handleEvent: HandleEvent = {

    case evt: FortuneEvent ⇒ evt match {
      case LiabilityTaken(user, liabilityId, liability, initializer, metadata, comment) =>
        liabilitiesRepo.insert(liabilityId → evt.aggregateId, liability)
      case LiabilityPaidOff(user, liabilityId, amount, metadata, comment) => for {
        l ← liabilitiesRepo.byId(liabilityId → evt.aggregateId)
        r ← l match {
          case Some(liab) if liab.worth.amount <= amount ⇒ liabilitiesRepo.remove(liabilityId → evt.aggregateId)
          case Some(liab) ⇒ liabilitiesRepo.replaceById(liabilityId → evt.aggregateId, liab.payOff(amount))
          case None ⇒ Future.successful(())
        }
      } yield r

      case _ ⇒ Future.successful(())
    }
  }
}
