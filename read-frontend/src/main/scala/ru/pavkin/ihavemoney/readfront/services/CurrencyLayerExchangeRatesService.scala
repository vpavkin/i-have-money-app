package ru.pavkin.ihavemoney.readfront.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import cats.instances.either._
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.eq._
import cats.syntax.traverse._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.parser.decode
import ru.pavkin.ihavemoney.domain.fortune.Currency.RUR
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExchangeRate, ExchangeRates}
import ru.pavkin.ihavemoney.readfront.services.CurrencyLayerExchangeRatesService._

import scala.concurrent.{ExecutionContext, Future}

class CurrencyLayerExchangeRatesService(
  accessKey: String)(
  implicit
  E: ExecutionContext,
  A: ActorSystem,
  M: Materializer)
  extends CurrencyExchangeRatesService[Result]
    with FailFastCirceSupport {

  def queryRates(currencies: List[Currency]): Future[Either[Throwable, ExchangeRates]] = {
    val url = s"http://apilayer.net/api/live?access_key=$accessKey&currencies=${currencies.map(adaptCurrency).mkString(",")}"
    Http().singleRequest(HttpRequest(uri = Uri(url)))
      .flatMap(response => response.status match {
        case OK => Unmarshal(response.entity).to[ExchangeRates].map(Right(_))
        case other => Unmarshal(response.entity).to[String]
          .map(body => Left(new Exception(s"Unexpected response code: $other, body: $body")))
      })
  }
}

object CurrencyLayerExchangeRatesService {
  type Result[T] = Future[Either[Throwable, T]]

  private def adaptCurrency(c: Currency): String = c match {
    case RUR => "RUB"
    case other => other.code
  }

  private def adaptCode(code: String): Either[String, Currency] = code match {
    case "RUB" => Right(RUR)
    case other => Either.fromOption(Currency.withNameOption(other), s"Unknown currency code: $other")
  }

  private[services] implicit val decoder: Decoder[ExchangeRates] = RawRates.rawRatesDecoder.emap(rawRates =>
    for {
      base <- adaptCode(rawRates.source)
      rates <- rawRates.quotes.map {
        case (fromto, rate) => for {
          from <- adaptCode(fromto.take(3))
          to <- adaptCode(fromto.drop(3))
        } yield ExchangeRate(from, to, rate)
      }.toList.sequenceU.map(_.filterNot(r => r.from === r.to))
    } yield ExchangeRates(base, rates)
  )


  private[services] case class RawRates(source: String, quotes: Map[String, BigDecimal])

  private[services] object RawRates {
    implicit val rawRatesDecoder = deriveDecoder[RawRates]
  }
}
