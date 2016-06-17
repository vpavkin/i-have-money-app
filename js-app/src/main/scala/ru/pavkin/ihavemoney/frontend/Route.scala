package ru.pavkin.ihavemoney.frontend

sealed trait Route

object Route {
  case object Preloader extends Route
  case object Login extends Route
  case object NoFortunes extends Route
  case object AddTransactions extends Route
  case object BalanceView extends Route
  case object TransactionLogView extends Route
}
