package ru.pavkin.ihavemoney.domain.fortune

import cats.syntax.eq._

case class ExchangeRate(from: Currency, to: Currency, rate: BigDecimal) {
  override def toString: String = s"${from.code}/${to.code} = $rate"
}

case class ExchangeRates(baseCurrency: Currency, rates: List[ExchangeRate]) {
  def findRate(from: Currency, to: Currency): Option[BigDecimal] =
    if (from === to) Some(BigDecimal(1.0))
    else findFlatRate(from, to).orElse(findIndirectRate(from, to))

  private def findDirectRate(from: Currency, to: Currency) =
    rates.find(r => r.from === from && r.to === to).map(_.rate)

  private def findInvertRate(from: Currency, to: Currency) =
    rates.find(r => r.from === to && r.to === from).map(BigDecimal(1.0) / _.rate)

  private def findFlatRate(from: Currency, to: Currency) =
    findDirectRate(from, to).orElse(findInvertRate(from, to))

  private def findIndirectRate(from: Currency, to: Currency) =
    for {
      x <- findFlatRate(baseCurrency, from)
      y <- findFlatRate(baseCurrency, to)
    } yield y / x


  def exchange(amount: BigDecimal, from: Currency, to: Currency): Option[BigDecimal] =
    findRate(from, to).map(_ * amount)

  def exchangeUnsafe(amount: BigDecimal, from: Currency, to: Currency): BigDecimal =
    exchange(amount, from, to).getOrElse(throw new Exception(s"Unknown currency pair: $from/$to"))

  override def toString = s"[${rates.mkString(",")}]"
}

object ExchangeRates {
  val Empty = ExchangeRates(Currency.USD, Nil)
}
