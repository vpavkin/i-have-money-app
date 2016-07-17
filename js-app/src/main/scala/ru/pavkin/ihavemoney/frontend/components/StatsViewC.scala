package ru.pavkin.ihavemoney.frontend.components

import java.time.{LocalDate, Year, YearMonth}

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.bootstrap.Panel
import ru.pavkin.ihavemoney.frontend.gravatar.GravatarAPI
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadEventLog
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.ihavemoney.protocol.Event
import ru.pavkin.utils.date._

import scalacss.ScalaCssReact._

object StatsViewC {

  case class Props(log: ModelProxy[Pot[List[Event]]])
  case class State(month: YearMonth, year: Year)

  class Backend($: BackendScope[Props, State]) {

    def loadTransactionLog(pr: Props) = Callback {
      AppCircuit.dispatch(LoadEventLog())
    }

    def amountStyle(amount: BigDecimal) =
      if (amount >= 0) logPosAmount
      else logNegAmount

    def render(pr: Props) = div(
      pr.log().renderEmpty(PreloaderC()),
      pr.log().renderPending(_ => div(PreloaderC())),
      pr.log().renderReady(log ⇒
        div(common.container,
          div(grid.columnAll(GRID_SIZE / 2),
            Panel(Some(h3(common.panelTitle, "Monthly stats")),
              common.context.default, "ololo"

            )
          )
        )
      )
    )
  }

  val component = ReactComponentB[Props]("StatsComponent")
      .initialState(State(YearMonth.now, Year.now))
      .renderBackend[Backend]
      .componentDidMount(s ⇒ s.backend.loadTransactionLog(s.props))
      .build
}
