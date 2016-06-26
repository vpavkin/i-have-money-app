package ru.pavkin.ihavemoney.frontend.redux

object connectors {

  val activeRequest = AppCircuit.connect(_.activeRequest)
  val modal = AppCircuit.connect(_.modal)

  val fortunes = AppCircuit.connect(_.fortunes)
  val balances = AppCircuit.connect(_.balances)
  val assets = AppCircuit.connect(_.assets)
  val log = AppCircuit.connect(_.log)
}
