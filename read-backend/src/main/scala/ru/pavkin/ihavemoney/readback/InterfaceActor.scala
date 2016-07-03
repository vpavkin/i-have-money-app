package ru.pavkin.ihavemoney.readback

import akka.actor.Actor
import io.funcqrs.akka.EventsSourceProvider
import ru.pavkin.ihavemoney.domain.fortune.FortuneId
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.FortuneEvent
import ru.pavkin.ihavemoney.domain.query._
import ru.pavkin.ihavemoney.domain.user.UserId
import ru.pavkin.ihavemoney.readback.projections.{CategoriesViewProjection, FortuneAdjustmentsProjection, FortuneInfoProjection, FortunesPerUserProjection}
import ru.pavkin.ihavemoney.readback.repo.{AssetsViewRepository, LiabilitiesViewRepository, MoneyViewRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class InterfaceActor(
    fortuneEventsProvider: EventsSourceProvider,
    moneyRepo: MoneyViewRepository,
    assetsRepo: AssetsViewRepository,
    liabRepo: LiabilitiesViewRepository,
    categoriesRepo: CategoriesViewProjection.Repo,
    userRegistryRepo: FortunesPerUserProjection.Repo,
    fortuneInfoRepo: FortuneInfoProjection.Repo) extends Actor {
  implicit val dispatcher: ExecutionContext = context.system.dispatcher

  def hasAccess(user: UserId, toFortune: FortuneId): Future[Boolean] =
    userRegistryRepo.byId(user).map(_.exists(_.contains(toFortune)))

  def checkAccess[Q <: FortuneQuery](q: Q, handler: ⇒ Future[QueryResult]): Future[QueryResult] =
    hasAccess(q.user, q.fortuneId).flatMap {
      case true ⇒ handler
      case false ⇒ Future.successful(AccessDenied(q.id, s"User ${q.user} doesn't have access to fortune ${q.fortuneId}"))
    }

  def log(id: FortuneId): Future[List[FortuneEvent]] = new FortuneAdjustmentsProjection(id, fortuneEventsProvider).run

  def receive: Receive = {
    case query: Query ⇒
      val origin = sender
      val queryFuture: Future[QueryResult] = query match {
        case Fortunes(_, userId) ⇒
          userRegistryRepo.byId(userId).flatMap {
            case m if !m.exists(_.nonEmpty) ⇒ Future.successful(FortunesQueryResult(userId, Nil))
            case Some(fortunes) ⇒
              Future.sequence(fortunes.map(fortuneInfoRepo.byId))
                  .map(_.flatten.toList)
                  .map(FortunesQueryResult(userId, _))
          }

        case q@TransactionLog(_, uid, fortuneId) ⇒ checkAccess(q,
          log(fortuneId).map(TransactionLogQueryResult(fortuneId, _))
        )
        case q@Categories(_, uid, fortuneId) ⇒ checkAccess(q,
          categoriesRepo.byId(fortuneId).map {
            case m if m.isEmpty ⇒ CategoriesQueryResult(fortuneId, Nil, Nil)
            case Some((inc, exp)) ⇒ CategoriesQueryResult(fortuneId, inc.toList, exp.toList)
          }
        )
        case q@MoneyBalance(_, uid, fortuneId) ⇒ checkAccess(q,
          moneyRepo.findAll(fortuneId).map {
            case m if m.isEmpty ⇒ EntityNotFound(q.id, s"Fortune $fortuneId not found")
            case m ⇒ MoneyBalanceQueryResult(fortuneId, m)
          }
        )
        case q@Assets(id, uid, fortuneId) => checkAccess(q,
          assetsRepo.findAll(fortuneId)
              .map(_.map { case (k, v) ⇒ k.value.toString -> v })
              .map(AssetsQueryResult(fortuneId, _))
        )
        case q@Liabilities(id, uid, fortuneId) => checkAccess(q,
          liabRepo.findAll(fortuneId)
              .map(_.map { case (k, v) ⇒ k.value.toString -> v })
              .map(LiabilitiesQueryResult(fortuneId, _))
        )
      }
      queryFuture.onComplete {
        case Success(r) ⇒ origin ! r
        case Failure(ex) ⇒ origin ! QueryFailed(query.id, ex.getMessage)
      }
  }
}
