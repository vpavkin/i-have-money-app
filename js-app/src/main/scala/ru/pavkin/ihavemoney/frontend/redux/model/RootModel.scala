package ru.pavkin.ihavemoney.frontend.redux.model

import diode.data.Pot
import japgolly.scalajs.react.ReactElement
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency, Liability}
import ru.pavkin.ihavemoney.frontend.Route
import ru.pavkin.ihavemoney.protocol.{Auth, Transaction}

case class RootModel(auth: Option[Auth],
                     fortunes: Pot[List[String]] = Pot.empty,
                     balances: Pot[Map[Currency, BigDecimal]] = Pot.empty,
                     assets: Pot[Map[String, Asset]] = Pot.empty,
                     liabilities: Pot[Map[String, Liability]] = Pot.empty,
                     log: Pot[List[Transaction]] = Pot.empty,
                     initializerRedirectsTo: Option[Route] = None,
                     modal: Option[ReactElement] = None,
                     activeRequest: Pot[Unit] = Pot.empty) {

  def fortuneId: String = fortunes.get.head
}


