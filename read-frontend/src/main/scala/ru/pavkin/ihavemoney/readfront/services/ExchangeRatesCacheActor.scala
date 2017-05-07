package ru.pavkin.ihavemoney.readfront.services

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.PipeToSupport
import ru.pavkin.ihavemoney.domain.cache.MutableCache
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExchangeRates}
import ru.pavkin.ihavemoney.readfront.services.ExchangeRatesCacheActor._

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class ExchangeRatesCacheActor(
  service: CurrencyExchangeRatesService[CurrencyLayerExchangeRatesService.Result],
  cache: MutableCache[ExchangeRates])(
  implicit
  ec: ExecutionContext) extends Actor with PipeToSupport with ActorLogging {
  def receive: Receive = {

    case m: In => m match {

      case ReloadRates =>
        log.info(s"Rates reload request received...")
        val origin = sender
        service.queryRates(Currency.values.toList).map {
          case Left(error) =>
            log.error(error, "Failed to refresh exchange rates")
            RatesObtained(origin, cache.state)
          case Right(rates) =>
            log.info(s"Exchange rates refreshed: $rates")
            RatesObtained(origin, rates)
        }.recover {
          case NonFatal(error) =>
            log.error(error, "Failed to refresh exchange rates")
            RatesObtained(origin, cache.state)
        }
          .pipeTo(self)

      case RequestRates =>
        sender ! Rates(cache.state)
    }

    case m: Internal => m match {
      case RatesObtained(origin, rates) =>
        cache.update(rates)
        log.info(s"Rates cache updated")
        origin ! ReloadDone
    }
  }
}

object ExchangeRatesCacheActor {
  sealed trait In
  case object ReloadRates extends In
  case object RequestRates extends In

  sealed trait Out
  case object ReloadDone extends Out
  case class Rates(rates: ExchangeRates) extends Out

  private sealed trait Internal
  private case class RatesObtained(origin: ActorRef, rates: ExchangeRates) extends Internal
}
