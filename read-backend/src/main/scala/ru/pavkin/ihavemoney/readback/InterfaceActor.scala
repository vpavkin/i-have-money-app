package ru.pavkin.ihavemoney.readback

import akka.actor.Actor
import ru.pavkin.ihavemoney.domain.query._
import ru.pavkin.ihavemoney.readback.repo.{AssetsViewRepository, LiabilitiesViewRepository, MoneyViewRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class InterfaceActor(moneyRepo: MoneyViewRepository,
                     assetsRepo: AssetsViewRepository,
                     liabRepo: LiabilitiesViewRepository) extends Actor {
  implicit val dispatcher: ExecutionContext = context.system.dispatcher

  def receive: Receive = {
    case q: Query ⇒
      val origin = sender
      val queryFuture: Future[QueryResult] = q match {
        case MoneyBalance(_, fortuneId) ⇒
          moneyRepo.findAll(fortuneId).map {
            case m if m.isEmpty ⇒ EntityNotFound(q.id, s"Fortune $fortuneId not found")
            case m ⇒ MoneyBalanceQueryResult(fortuneId, m)
          }
        case Assets(id, fortuneId) =>
          assetsRepo.findAll(fortuneId)
            .map(_.map { case (k, v) ⇒ k.value.toString -> v })
            .map(AssetsQueryResult(fortuneId, _))
        case Liabilities(id, fortuneId) =>
          liabRepo.findAll(fortuneId)
            .map(_.map { case (k, v) ⇒ k.value.toString -> v })
            .map(LiabilitiesQueryResult(fortuneId, _))
      }
      queryFuture.onComplete {
        case Success(r) ⇒ origin ! r
        case Failure(ex) ⇒ origin ! QueryFailed(q.id, ex.getMessage)
      }
  }
}
