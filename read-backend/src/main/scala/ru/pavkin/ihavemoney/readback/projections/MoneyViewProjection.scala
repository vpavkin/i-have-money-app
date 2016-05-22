package ru.pavkin.ihavemoney.readback.projections

import io.funcqrs.{HandleEvent, Projection}
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, FortuneId}
import ru.pavkin.ihavemoney.readback.repo.{AssetsViewRepository, LiabilitiesViewRepository, MoneyViewRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MoneyViewProjection(moneyRepo: MoneyViewRepository,
                          assetRepo: AssetsViewRepository,
                          liabilityRepo: LiabilitiesViewRepository) extends Projection {

  private def adjustFortune(id: FortuneId, currency: Currency, op: Option[BigDecimal] ⇒ BigDecimal): Future[Unit] = {
    moneyRepo.byId(id → currency).flatMap {
      case Some(amount) ⇒ moneyRepo.updateById(id → currency, op(Some(amount)))
      case None ⇒ moneyRepo.insert(id → currency, op(None))
    }
  }

  def handleEvent: HandleEvent = {

    case evt: FortuneEvent ⇒ evt match {
      case e: FortuneIncreased ⇒
        adjustFortune(e.aggregateId, e.currency, _.getOrElse(BigDecimal(0.0)) + e.amount)
      case e: FortuneSpent ⇒
        adjustFortune(e.aggregateId, e.currency, _.getOrElse(BigDecimal(0.0)) - e.amount)
      case e: CurrencyExchanged =>
        adjustFortune(e.aggregateId, e.fromCurrency, _.getOrElse(BigDecimal(0.0)) - e.fromAmount)
          .flatMap(_ ⇒ adjustFortune(e.aggregateId, e.toCurrency, _.getOrElse(BigDecimal(0.0)) + e.toAmount))
      case e: AssetAcquired ⇒
        adjustFortune(e.aggregateId, e.asset.currency, _.getOrElse(BigDecimal(0.0)) - e.asset.worth.amount)
      case e: AssetSold ⇒
        assetRepo.byId(e.assetId → e.aggregateId).flatMap(_.map(a ⇒
          adjustFortune(e.aggregateId, a.currency, _.getOrElse(BigDecimal(0.0)) + a.worth.amount)
        ).getOrElse(Future.successful(())))
      case e: LiabilityTaken =>
        adjustFortune(e.aggregateId, e.liability.currency, _.getOrElse(BigDecimal(0.0)) - e.liability.amount)
      case e: LiabilityPaidOff =>
        liabilityRepo.byId(e.liabilityId → e.aggregateId).flatMap(_.map(a ⇒
          adjustFortune(e.aggregateId, a.currency, _.getOrElse(BigDecimal(0.0)) - a.amount)
        ).getOrElse(Future.successful(())))
      case _ ⇒ Future.successful(())
    }
  }
}
