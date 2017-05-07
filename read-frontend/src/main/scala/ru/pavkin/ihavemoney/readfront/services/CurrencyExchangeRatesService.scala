package ru.pavkin.ihavemoney.readfront.services

import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExchangeRates}

trait CurrencyExchangeRatesService[F[_]] {
  def queryRates(currencies: List[Currency]): F[ExchangeRates]
}
