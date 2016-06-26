package ru.pavkin.ihavemoney.frontend.components

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency, Liability}
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadAssets, LoadBalances, LoadLiabilities}
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scalacss.ScalaCssReact._

object BalanceViewC {

  case class Props(balances: ModelProxy[Pot[Map[Currency, BigDecimal]]],
                   assets: ModelProxy[Pot[Map[String, Asset]]],
                   liabilities: ModelProxy[Pot[Map[String, Liability]]])

  class Backend($: BackendScope[Props, Unit]) {

    def loadData(pr: Props) = Callback {
      AppCircuit.dispatch(LoadBalances())
      AppCircuit.dispatch(LoadAssets())
      AppCircuit.dispatch(LoadLiabilities())
    }

    def render(pr: Props) = div(common.container,
      div(grid.columnAll(GRID_SIZE / 3), className := "panel",
        pr.balances().renderEmpty(PreloaderC()),
        pr.balances().renderPending(_ => PreloaderC()),
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
      ),
      div(grid.columnAll(GRID_SIZE / 3), className := "panel",
        pr.assets().renderEmpty(PreloaderC()),
        pr.assets().renderPending(_ => PreloaderC()),
        pr.assets().renderReady(assets ⇒
          div(
            table(className := "table table-striped table-hover table-condensed",
              thead(tr(th("Asset"), th("Worth"))),
              tbody(assets.values.map(asset ⇒ tr(td(asset.name), td(asset.worth.toString))))
            )
          )
        )
      ),
      div(grid.columnAll(GRID_SIZE / 3), className := "panel",
        pr.liabilities().renderEmpty(PreloaderC()),
        pr.liabilities().renderPending(_ => PreloaderC()),
        pr.liabilities().renderReady(liabilities ⇒
          div(
            table(className := "table table-striped table-hover table-condensed",
              thead(tr(th("Liability"), th("Worth"))),
              tbody(liabilities.values.map(asset ⇒ tr(td(asset.name), td(asset.worth.toString))))
            )
          )
        )
      )
    )
  }

  val component = ReactComponentB[Props]("Balance sheet")
    .renderBackend[Backend]
    .componentDidMount(s ⇒ s.backend.loadData(s.props))
    .build
}
