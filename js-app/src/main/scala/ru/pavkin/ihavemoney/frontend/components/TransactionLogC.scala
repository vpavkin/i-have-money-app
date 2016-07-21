package ru.pavkin.ihavemoney.frontend.components

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.bootstrap.{Checkbox, Panel}
import ru.pavkin.ihavemoney.frontend.bootstrap.attributes._
import ru.pavkin.ihavemoney.frontend.gravatar.GravatarAPI
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadCategories, LoadEventLog}
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.protocol.{Event, Transaction}
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.utils.date._

import scalacss.ScalaCssReact._

object TransactionLogC {

  case class Props(
      log: ModelProxy[Pot[List[Event]]],
      categories: ModelProxy[Pot[Categories]])

  case class State(filterByCategory: Boolean = false, category: String = "Groceries")

  class Backend($: BackendScope[Props, State]) {

    def loadData(pr: Props) = Callback {
      AppCircuit.dispatch(LoadEventLog())
      AppCircuit.dispatch(LoadCategories())
    }

    def amountStyle(amount: BigDecimal) =
      if (amount >= 0) logPosAmount
      else logNegAmount

    def render(pr: Props, st: State) = div(common.container,
      div(grid.columnAll(2),
        Panel(Some(h3(common.panelTitle, "Filters")),
          common.context.info,
          Checkbox(isChecked ⇒ $.modState(_.copy(filterByCategory = isChecked)), st.filterByCategory, "By category"),
          pr.categories().renderReady(cats =>
            StringValueSelector(st.category, s => $.modState(_.copy(category = s)), cats.expense.sorted, common.context.info)
          ))
      ),
      div(grid.columnAll(10),
        Panel(Some(h3(common.panelTitle, "Transactions")),
          common.context.default,
          pr.log().renderEmpty(PreloaderC()),
          pr.log().renderPending(_ => PreloaderC()),
          pr.log().renderReady { log ⇒
            val transactions = log.collect {
              case t: Transaction => t
            }.filter(t => if (st.filterByCategory) t.category == st.category else true)

            div(
              table(className := "table table-striped table-hover table-condensed",
                thead(tr(th(""), th("Date"), th("Category"), th("Amount"), th("Comment"))),
                tbody(
                  transactions.zipWithIndex.map {
                    case (t, index) ⇒ tr(
                      key := index.toString,
                      td(width := "30px", paddingTop := "0px", paddingBottom := "0px", verticalAlign := "middle",
                        img(src := GravatarAPI.img(t.user, 20), className := "img-circle", title := t.user)),
                      td(t.date.ddmmyyyy),
                      td(t.category),
                      td(amountStyle(t.amount), t.amount.toString + t.currency.sign),
                      td(t.comment.getOrElse(""): String)
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
