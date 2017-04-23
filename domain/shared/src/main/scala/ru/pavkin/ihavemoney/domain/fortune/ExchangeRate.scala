package ru.pavkin.ihavemoney.domain.fortune

import cats.syntax.eq._

case class ExchangeRate(from: Currency, to: Currency, rate: BigDecimal)

case class ExchangeRates(rates: List[ExchangeRate]) {
  def findRate(from: Currency, to: Currency): Option[BigDecimal] =
    if (from === to) Some(BigDecimal(1.0))
    else rates.find(r => r.from === from && r.to === to).map(_.rate)
      .orElse(rates.find(r => r.from === to && r.to === from).map(BigDecimal(1.0) / _.rate))

  def exchange(amount: BigDecimal, from: Currency, to: Currency): Option[BigDecimal] =
    findRate(from, to).map(_ * amount)
}

object ExchangeRates {

  // todo: temporary stub
  val Default = ExchangeRates(List(
    ExchangeRate(Currency.EUR, Currency.RUR, 60.56),
    ExchangeRate(Currency.USD, Currency.RUR, 56.46),
    ExchangeRate(Currency.EUR, Currency.USD, 1.07)
  ))
}
