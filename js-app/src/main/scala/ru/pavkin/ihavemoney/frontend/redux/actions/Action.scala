package ru.pavkin.ihavemoney.frontend.redux.actions

import cats.data.Xor
import diode.data.{Empty, Pot, PotAction}
import japgolly.scalajs.react.ReactElement
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency, FortuneInfo, Liability}
import ru.pavkin.ihavemoney.frontend.Route
import ru.pavkin.ihavemoney.protocol.{Auth, RequestError, Transaction}

import scala.concurrent.Future

sealed trait Action extends diode.Action

case class UpdateFortuneId(potResult: Pot[List[FortuneInfo]] = Pot.empty) extends Action
  with PotAction[List[FortuneInfo], UpdateFortuneId] {
  def next(newResult: Pot[List[FortuneInfo]]): UpdateFortuneId = copy(potResult = newResult)
}

case class LoadBalances(potResult: Pot[Map[Currency, BigDecimal]] = Pot.empty)
  extends Action with PotAction[Map[Currency, BigDecimal], LoadBalances] {
  def next(newResult: Pot[Map[Currency, BigDecimal]]): LoadBalances = copy(potResult = newResult)
}

case class LoadAssets(potResult: Pot[Map[String, Asset]] = Pot.empty)
  extends Action with PotAction[Map[String, Asset], LoadAssets] {
  def next(newResult: Pot[Map[String, Asset]]): LoadAssets = copy(potResult = newResult)
}

case class LoadLiabilities(potResult: Pot[Map[String, Liability]] = Pot.empty)
  extends Action with PotAction[Map[String, Liability], LoadLiabilities] {
  def next(newResult: Pot[Map[String, Liability]]): LoadLiabilities = copy(potResult = newResult)
}

case class LoadTransactionLog(potResult: Pot[List[Transaction]] = Pot.empty)
  extends Action with PotAction[List[Transaction], LoadTransactionLog] {
  def next(newResult: Pot[List[Transaction]]): LoadTransactionLog = copy(potResult = newResult)
}

case class LoggedIn(auth: Auth) extends Action
case object LoggedOut extends Action

case class ShowModal(modal: ReactElement) extends Action
case object HideModal extends Action

case class SendRequest(command: Future[Xor[RequestError, Unit]], potResult: Pot[Unit] = Empty)
  extends Action with PotAction[Unit, SendRequest] {
  def next(newResult: Pot[Unit]): SendRequest = copy(potResult = newResult)
}

case class SetInitializerRedirect(route: Route) extends Action
