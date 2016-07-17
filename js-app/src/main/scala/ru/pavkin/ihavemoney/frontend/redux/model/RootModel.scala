package ru.pavkin.ihavemoney.frontend.redux.model

import diode.data.Pot
import japgolly.scalajs.react.ReactElement
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency, FortuneInfo, Liability}
import ru.pavkin.ihavemoney.frontend.Route
import ru.pavkin.ihavemoney.protocol.{Auth, Event}

case class RootModel(
    auth: Option[Auth],
    fortunes: Pot[List[FortuneInfo]] = Pot.empty,
    categories: Pot[Categories] = Pot.empty,
    balances: Pot[Map[Currency, BigDecimal]] = Pot.empty,
    assets: Pot[Map[String, Asset]] = Pot.empty,
    liabilities: Pot[Map[String, Liability]] = Pot.empty,
    log: Pot[List[Event]] = Pot.empty,
    initializerRedirectsTo: Option[Route] = None,
    modal: Option[ReactElement] = None,
    activeRequest: Pot[Unit] = Pot.empty) {

  def fortuneInfo: FortuneInfo = fortunes.get.head
  def fortuneId: String = fortuneInfo.id
}

case class Categories(income: List[String], expense: List[String])

