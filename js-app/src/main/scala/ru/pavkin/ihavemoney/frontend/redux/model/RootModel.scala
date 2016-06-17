package ru.pavkin.ihavemoney.frontend.redux.model

import diode.data.Pot
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.protocol.{Auth, Transaction}

case class RootModel(auth: Option[Auth],
                     fortuneId: Option[String] = None,
                     balances: Pot[Map[Currency, BigDecimal]] = Pot.empty,
                     log: Pot[List[Transaction]] = Pot.empty)


