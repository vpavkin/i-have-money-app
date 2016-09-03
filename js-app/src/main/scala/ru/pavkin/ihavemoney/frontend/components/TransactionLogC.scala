package ru.pavkin.ihavemoney.frontend.components

import java.time.YearMonth

import cats.data.Xor
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.window
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Checkbox, Icon, Panel}
import ru.pavkin.ihavemoney.frontend.gravatar.GravatarAPI
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadCategories, LoadEventLog}
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.ihavemoney.protocol.{Event, Transaction}
import ru.pavkin.utils.date._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.ScalaCssReact._

object TransactionLogC {

  case class Props(
      log: ModelProxy[Pot[List[Event]]],
      categories: ModelProxy[Pot[Categories]])

  case class State(
      filterByCategory: Boolean = false,
      category: String = "Groceries",
      filterByMonth: Boolean = false,
      month: YearMonth = YearMonth.now(),
      textFilter: String = "")

  class Backend($: BackendScope[Props, State]) {

    def loadData(pr: Props) = Callback {
      AppCircuit.dispatch(LoadEventLog())
      AppCircuit.dispatch(LoadCategories())
    }

    def amountStyle(amount: BigDecimal) =
      if (amount >= 0) logPosAmount
      else logNegAmount

    def onTextChange(change: (State, String) ⇒ State)(e: ReactEventI) = {
      val newValue = e.target.value
      applyStateChange(change)(newValue)
    }

    def onCancelClick(t: Transaction) = Callback {
      val response = window.confirm(s"Are you sure to cancel transaction ${t.amount.toString}${t.currency.sign}, ${t.category} from ${t.date.ddmmyyyy}?")
      if (response)
        api.cancelTransaction(t.id).map {
          case Xor.Left(error) ⇒ Callback.alert(s"Error: $error")
          case _ ⇒ Callback.alert(s"Success, refresh the page!")
        }
    }

    def applyStateChange[T](change: (State, T) ⇒ State)(newValue: T): Callback =
      $.modState(change(_, newValue))

    def render(pr: Props, st: State) = div(common.container,
      div(grid.columnAll(2),
        Panel(Some(h3(common.panelTitle, "Filters")),
          common.context.info,
          Checkbox(isChecked ⇒ $.modState(_.copy(filterByCategory = isChecked)), st.filterByCategory, "By category"),
          pr.categories().renderReady(cats =>
            StringValueSelector(st.category, s => $.modState(_.copy(category = s)), cats.expense.sorted, common.context.info)
          ),
          hr(),
          Checkbox(isChecked ⇒ $.modState(_.copy(filterByMonth = isChecked)), st.filterByMonth, "By month"),
          YearMonthSelector(st.month, onChange = ym => $.modState(_.copy(month = ym))),
          hr(),
          label("Text search:"),
          input.text(common.formControl, value := st.textFilter, onChange ==> onTextChange((s, v) => s.copy(textFilter = v)))
        )
      ),
      div(grid.columnAll(10),
        Panel(Some(h3(common.panelTitle, "Transactions")),
          common.context.default,
          pr.log().renderEmpty(PreloaderC()),
          pr.log().renderPending(_ => PreloaderC()),
          pr.log().renderReady { log ⇒
            val transactions = log.collect {
              case t: Transaction => t
            }.filter(t =>
              (!st.filterByCategory || t.category == st.category)
                  && (!st.filterByMonth || (t.date.getMonth == st.month.getMonth && t.date.getYear == st.month.getYear))
                  && (st.textFilter.isEmpty || t.comment.exists(_.toLowerCase.contains(st.textFilter.toLowerCase)))
            )

            div(
              table(className := "table table-striped table-hover table-condensed",
                thead(tr(th(""), th("Date"), th("Category"), th("Amount"), th("Comment"), th(""))),
                tbody(
                  transactions.zipWithIndex.map {
                    case (t, index) ⇒ tr(
                      key := index.toString,
                      td(width := "30px", paddingTop := "0px", paddingBottom := "0px", verticalAlign := "middle",
                        img(src := GravatarAPI.img(t.user, 20), className := "img-circle", title := t.user)),
                      td(t.date.ddmmyyyy),
                      td(t.category),
                      td(amountStyle(t.amount), t.amount.toString + t.currency.sign),
                      td(t.comment.getOrElse(""): String),
                      td(textAlign.right, span(Icon.timesCircle, color := "#E74C3C", onClick --> onCancelClick(t)))
                    )
                  }
                )
              )
            )
          }
        )
      )
    )
  }

  val component = ReactComponentB[Props]("LogComponent")
      .initialState(State())
      .renderBackend[Backend]
      .componentDidMount(s ⇒ s.backend.loadData(s.props))
      .build
}
