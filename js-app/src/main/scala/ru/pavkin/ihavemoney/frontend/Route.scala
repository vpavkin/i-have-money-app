package ru.pavkin.ihavemoney.frontend

sealed trait Route

object Route {
  case object Initializer extends Route
  case object Login extends Route
  case object NoFortunes extends Route
  case object Income extends Route
  case object Expenses extends Route
  case object Exchange extends Route
  case object BalanceView extends Route
  case object StatsView extends Route
  case object TransactionLogView extends Route
  case object FortuneSettingsView extends Route
}
