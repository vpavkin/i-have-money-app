package ru.pavkin.ihavemoney.readback.projections

import java.time.{Duration, OffsetDateTime}

import io.funcqrs.{HandleEvent, Projection}
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.readback.repo.TransactionsViewRepository

import scala.concurrent.{ExecutionContext, Future}

class RecentTransactionsProjection(
    transactionRepo: TransactionsViewRepository,
    period: Duration = Duration.ofDays(30))(
    implicit ec: ExecutionContext) extends Projection {

  def handleEvent: HandleEvent = {
    case evt: FortuneEvent ⇒
      evt match {
        case FortuneIncreased(user, amount, currency, category, initializer, metadata, comment) =>
          transactionRepo.insert(evt)
        case FortuneSpent(user, amount, currency, category, overrideDate, initializer, metadata, comment) =>
          transactionRepo.insert(evt)
        case CurrencyExchanged(user, fromAmount, fromCurrency, toAmount, toCurrency, metadata, comment) =>
          transactionRepo.insert(evt)
        case _ ⇒ Future.successful(())
      }
      transactionRepo.removeOlderThan(OffsetDateTime.now().minus(period))
  }
}
