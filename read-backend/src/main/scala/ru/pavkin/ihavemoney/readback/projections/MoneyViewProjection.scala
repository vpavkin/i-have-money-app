package ru.pavkin.ihavemoney.readback.projections

import io.funcqrs.{HandleEvent, Projection}
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, FortuneId}
import ru.pavkin.ihavemoney.readback.repo.{AssetsViewRepository, LiabilitiesViewRepository, MoneyViewRepository, TransactionsViewRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MoneyViewProjection(
    recentTransactionsRepo: TransactionsViewRepository,
    moneyRepo: MoneyViewRepository,
    assetRepo: AssetsViewRepository,
    liabilityRepo: LiabilitiesViewRepository) extends Projection {

  private def adjustFortune(id: FortuneId, currency: Currency, op: BigDecimal ⇒ BigDecimal): Future[Unit] = {
    moneyRepo.byId(id → currency).flatMap {
      case Some(amount) ⇒ moneyRepo.replaceById(id → currency, op(amount))
      case None ⇒ moneyRepo.insert(id → currency, op(BigDecimal(0.0)))
    }
  }

  def handleEvent: HandleEvent = {

    case evt: FortuneEvent ⇒ evt match {
      case e: FortuneIncreased ⇒
        adjustFortune(e.aggregateId, e.currency, _ + e.amount)
      case e: FortuneSpent ⇒
        adjustFortune(e.aggregateId, e.currency, _ - e.amount)
      case e: CurrencyExchanged =>
        adjustFortune(e.aggregateId, e.fromCurrency, _ - e.fromAmount)
            .flatMap(_ ⇒ adjustFortune(e.aggregateId, e.toCurrency, _ + e.toAmount))
      case e: AssetAcquired ⇒
        adjustFortune(e.aggregateId, e.asset.currency, _ - e.asset.worth.amount)
      case e: AssetSold ⇒
        assetRepo.byId(e.assetId → e.aggregateId).flatMap(_.map(a ⇒
          adjustFortune(e.aggregateId, a.currency, _ + a.worth.amount)
        ).getOrElse(Future.successful(())))
      case e: LiabilityTaken if !e.initializer =>
        adjustFortune(e.aggregateId, e.liability.currency, _ + e.liability.amount)
      case e: LiabilityPaidOff =>
        liabilityRepo.byId(e.liabilityId → e.aggregateId).flatMap(_.map(a ⇒
          adjustFortune(e.aggregateId, a.currency, _ - e.amount)
        ).getOrElse(Future.successful(())))
      case e: TransactionCancelled =>
        recentTransactionsRepo.findTransaction(e.transactionId).map {
          case Some(FortuneIncreased(user, amount, currency, category, initializer, metadata, comment)) =>
            adjustFortune(e.aggregateId, currency, _ - amount)
          case Some(FortuneSpent(user, amount, currency, category, overrideDate, initializer, metadata, comment)) =>
            adjustFortune(e.aggregateId, currency, _ + amount)
          case Some(CurrencyExchanged(user, fromAmount, fromCurrency, toAmount, toCurrency, metadata, comment)) =>
            adjustFortune(e.aggregateId, fromCurrency, _ + fromAmount)
                .flatMap(_ ⇒ adjustFortune(e.aggregateId, toCurrency, _ - toAmount))
          case _ => ()
        }
      case _ ⇒ Future.successful(())
    }
  }
}
