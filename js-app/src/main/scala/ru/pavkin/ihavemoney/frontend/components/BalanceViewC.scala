package ru.pavkin.ihavemoney.frontend.components

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.{Asset, Currency, Liability, Worth}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup, Panel}
import ru.pavkin.ihavemoney.frontend.components.selectors.CurrencySelector
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadAssets, LoadBalances, LoadLiabilities}
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.Future
import scala.util.Try
import scalacss.ScalaCssReact._

object BalanceViewC {

  case class Props(
    balances: ModelProxy[Pot[Map[Currency, BigDecimal]]],
    assets: ModelProxy[Pot[Map[String, Asset]]],
    liabilities: ModelProxy[Pot[Map[String, Liability]]])

  case class State(correctionCurrency: Currency, correctionAmount: String) {
    def correctionWorth: Option[Worth] =
      Try(BigDecimal(correctionAmount)).toOption.map(Worth(_, correctionCurrency))
  }

  class Backend($: BackendScope[Props, State]) {

    def loadData(pr: Props) = Callback {
      AppCircuit.dispatch(LoadBalances())
      AppCircuit.dispatch(LoadAssets())
      AppCircuit.dispatch(LoadLiabilities())
    }

    private def onFormSubmit(e: ReactEventI) = e.preventDefaultCB

    private def onTextChange(change: (State, String) ⇒ State)(e: ReactEventI) = {
      val newValue = e.target.value
      applyStateChange(change)(newValue)
    }

    private def amountCurrencyHelper(oldState: State, newAmount: String): State = newAmount match {
      case s if s.endsWith("e") || s.endsWith("€") ⇒ oldState.copy(Currency.EUR, s.init)
      case s if s.endsWith("$") || s.endsWith("d") ⇒ oldState.copy(Currency.USD, s.init)
      case s if s.endsWith("r") ⇒ oldState.copy(Currency.RUR, s.init)
      case _ ⇒ oldState.copy(correctionAmount = newAmount)
    }

    private def applyStateChange[T](change: (State, T) ⇒ State)(newValue: T): Callback =
      $.modState(change(_, newValue))

    private def genSubmit[T](st: State)(req: ⇒ Future[T]): Callback =
      if (!isValid(st))
        Callback.alert("Invalid data")
      else Callback.future(req.map {
        case Left(error) ⇒ Callback.alert(s"Error: $error")
        case _ ⇒ Callback.alert(s"Success")
      })

    private def onCorrectionSubmit(state: State): Callback = genSubmit(state)(api.correct(
      BigDecimal(state.correctionAmount),
      state.correctionCurrency
    ))

    private def isValid(s: State) =
      Try(BigDecimal(s.correctionAmount)).toOption.exists(_ >= 0)

    private def amountStyle(amount: BigDecimal) =
      if (amount >= 0) logPosAmount
      else logNegAmount

    def render(pr: Props, state: State) = div(
      div(common.container,
        div(grid.columnAll(2),
          Panel(Some(h3(common.panelTitle, "Money balance")),
            common.context.info,
            pr.balances().renderEmpty(PreloaderC()),
            pr.balances().renderPending(_ => PreloaderC()),
            pr.balances().renderReady(balances ⇒
              div(
                table(className := "table table-striped table-hover table-condensed",
                  tbody(
                    balances.map {
                      case (currency, amount) ⇒ tr(td(currency.code), td(amount.toString))
                    }
                  )
                )
              )
            )
          )
        ),
        div(grid.columnAll(5),
          Panel(Some(h3(common.panelTitle, "Assets")),
            common.context.success,
            pr.assets().renderEmpty(PreloaderC()),
            pr.assets().renderPending(_ => PreloaderC()),
            pr.assets().renderReady(assets ⇒
              div(
                table(className := "table table-striped table-hover table-condensed",
                  thead(tr(th("Asset"), th("Worth"))),
                  tbody(assets.values.map(asset ⇒ tr(td(asset.name), td(asset.worth.toPrettyString))))
                )
              )
            )
          )
        ),
        div(grid.columnAll(5),
          Panel(Some(h3(common.panelTitle, "Liabilities")),
            common.context.danger,
            pr.liabilities().renderEmpty(PreloaderC()),
            pr.liabilities().renderPending(_ => PreloaderC()),
            pr.liabilities().renderReady(liabilities ⇒
              div(
                table(className := "table table-striped table-hover table-condensed",
                  thead(tr(th("Liability"), th("Worth"))),
                  tbody(liabilities.values.map(asset ⇒ tr(td(asset.name), td(asset.worth.toPrettyString))))
                )
              )
            ))
        )
      ),
      div(common.container,
        div(grid.columnAll(GRID_SIZE / 2),
          Panel(Some(h3(common.panelTitle, "Balance correction")),
            common.context.default,
            form(
              common.formHorizontal,
              onSubmit ==> onFormSubmit,
              FormGroup(
                div(grid.columnAll(6),
                  div(className := "input-group",
                    input.text(
                      required := true,
                      common.formControl,
                      addonMainInput,
                      increasedFontSize, rightMargin,
                      placeholder := s"${state.correctionCurrency.code} balance",
                      value := state.correctionAmount,
                      onChange ==> onTextChange((s, v) ⇒ s.copy(correctionAmount = v))
                    ),
                    div(className := "input-group-btn",
                      CurrencySelector(
                        state.correctionCurrency,
                        c ⇒ applyStateChange[Currency]((st, v) ⇒ st.copy(correctionCurrency = v))(c),
                        Currency.values.toList,
                        style = common.context.info,
                        addAttributes = Seq(increasedFontSize, inputCurrencyAddon))
                    )
                  )
                ),
                div(grid.columnAll(2),
                  Button(onCorrectionSubmit(state),
                    style = common.context.success,
                    addAttributes = Seq(disabled := (!isValid(state))),
                    addStyles = Seq(increasedFontSize)
                  )("Correct")
                ),
                div(grid.columnAll(4),
                  pr.balances().renderReady(balances ⇒
                    if (isValid(state)) {
                      val c = state.correctionWorth.get
                      val delta = c.amount - balances.getOrElse(c.currency, BigDecimal(0))
                      div(fontSize := "32px", textAlign := "right", amountStyle(delta), f"$delta%1.2f")
                    }
                    else div()
                  )
                )
              )
            )
          )
        )
      )
    )
  }

  val component = ReactComponentB[Props]("Balance sheet")
    .initialState(State(Currency.EUR, ""))
    .renderBackend[Backend]
    .componentDidMount(s ⇒ s.backend.loadData(s.props))
    .build
}
