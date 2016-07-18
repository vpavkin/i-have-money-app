package ru.pavkin.ihavemoney.domain.fortune

case class ExchangeRate(from: Currency, to: Currency, rate: BigDecimal)

object ExchangeRate {
  // todo: temporary stub
  val defaults = List(
    ExchangeRate(Currency.EUR, Currency.RUR, 70.0),
    ExchangeRate(Currency.USD, Currency.RUR, 62),
    ExchangeRate(Currency.EUR, Currency.USD, 1.12)
  )
}
