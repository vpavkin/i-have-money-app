package ru.pavkin.ihavemoney.frontend.redux

import diode.data.Pot
import diode.react.ReactConnectProxy
import japgolly.scalajs.react.ReactElement
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency, FortuneInfo, Liability}
import ru.pavkin.ihavemoney.frontend.components.state.TransactionLogUIState
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.protocol.Event

object connectors {

  type PotProxy[T] = ReactConnectProxy[Pot[T]]
  type OptionProxy[T] = ReactConnectProxy[Option[T]]

  val activeRequest: PotProxy[Unit] = AppCircuit.connect(_.activeRequest)
  val modal: OptionProxy[ReactElement] = AppCircuit.connect(_.modal)

  val fortunes: PotProxy[List[FortuneInfo]] = AppCircuit.connect(_.fortunes)
  val balances: PotProxy[Map[Currency, BigDecimal]] = AppCircuit.connect(_.balances)
  val assets: PotProxy[Map[String, Asset]] = AppCircuit.connect(_.assets)
  val liabilities: PotProxy[Map[String, Liability]] = AppCircuit.connect(_.liabilities)
  val categories: PotProxy[Categories] = AppCircuit.connect(_.categories)
  val log: PotProxy[List[Event]] = AppCircuit.connect(_.log)

  val transactionLogUIState: ReactConnectProxy[TransactionLogUIState] = AppCircuit.connect(_.transactionLogUIState)
}
