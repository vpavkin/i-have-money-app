package ru.pavkin.ihavemoney.frontend.components

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadBalances

object BalanceViewC {

  case class Props(balances: ModelProxy[Pot[Map[Currency, BigDecimal]]])

  class Backend($: BackendScope[Props, Unit]) {

    def loadBalances(pr: Props) = Callback {
      AppCircuit.dispatch(LoadBalances())
    }

    def render(pr: Props) = {
      div(
        pr.balances().renderEmpty("Loading..."),
        pr.balances().renderPending(_ => div("Loading...")),
        pr.balances().renderReady(balances ⇒
          div(
            table(className := "table table-striped table-hover table-condensed",
              thead(tr(th("Currency"), th("Amount"))),
              tbody(
                balances.map {
                  case (currency, amount) ⇒ tr(td(currency.code), td(amount.toString))
                }
              )
            )
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("AddTransactionsComponent")
    .renderBackend[Backend]
    .componentDidMount(s ⇒ s.backend.loadBalances(s.props))
    .build
}
