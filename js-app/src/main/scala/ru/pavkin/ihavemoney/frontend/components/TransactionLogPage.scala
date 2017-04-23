package ru.pavkin.ihavemoney.frontend.components

import java.time.{Year, YearMonth}

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.window
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExpenseCategory}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Checkbox, Icon, Panel}
import ru.pavkin.ihavemoney.frontend.components.selectors.ExpenseCategorySelector
import ru.pavkin.ihavemoney.frontend.gravatar.GravatarAPI
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadCategories, LoadEventLog, SetTransactionLogUIState}
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.ihavemoney.protocol.{Event, Expense, Transaction}
import ru.pavkin.utils.date._
import cats.syntax.eq._
import cats.instances.int._
import cats.Order.catsKernelOrderingForOrder
import diode.ActionBatch
import ru.pavkin.ihavemoney.frontend.components.state.TransactionLogUIState

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.ScalaCssReact._
import ReactHelpers._

object TransactionLogPage {

  case class Props(
    year: Year,
    log: ModelProxy[Pot[List[Event]]],
    categories: ModelProxy[Pot[Categories]],
    uiStatePx: ModelProxy[TransactionLogUIState])
    extends RemoteDataProps[Props] with ResetableUIStateProps {

    def loadData: Callback = Callback(AppCircuit.dispatch(ActionBatch(
      LoadEventLog(year),
      LoadCategories()
    )))


    def resetUIState: Callback = dispatchStateChange(TransactionLogUIState.Default)

    def shouldReload(nextProps: Props): Boolean = year =!= nextProps.year

    def state: TransactionLogUIState = uiStatePx()

    def transactionFilter(t: Expense): Boolean =
      (!state.filterByCategory || t.category === state.category) &&
        (!state.filterByMonth || (t.date.getMonth == state.month.getMonth && t.date.getYear === state.month.getYear)) &&
        (state.textFilter.isEmpty || t.comment.exists(_.toLowerCase.contains(state.textFilter.toLowerCase)))

    def dispatchStateChange(newState: TransactionLogUIState): Callback =
      AppCircuit.dispatchCB(SetTransactionLogUIState(newState))
  }

  class Backend($: BackendScope[Props, Unit]) {

    private def onCancelClick(t: Transaction[_]) = Callback {
      val response = window.confirm(s"Are you sure to cancel transaction ${t.amount.toString}${t.currency.sign}, ${t.category} from ${t.date.ddmmyyyy}?")
      if (response)
        api.cancelTransaction(t.id).map {
          case Left(error) ⇒ Callback.alert(s"Error: $error")
          case _ ⇒ Callback.alert(s"Success, refresh the page!")
        }
    }

    def render(pr: Props) = div(common.container,
      div(grid.columnAll(2),
        Panel(Some(h3(common.panelTitle, "Filters")),
          common.context.info,
          Checkbox(
            isChecked ⇒ pr.dispatchStateChange(pr.state.copy(filterByCategory = isChecked)),
            pr.state.filterByCategory,
            "By category"),
          pr.categories().renderReady(cats =>
            ExpenseCategorySelector(
              pr.state.category,
              s => pr.dispatchStateChange(pr.state.copy(category = s)),
              cats.expense.sorted,
              common.context.info)
          ),
          hr(),
          Checkbox(
            isChecked ⇒ pr.dispatchStateChange(pr.state.copy(filterByMonth = isChecked)),
            pr.state.filterByMonth,
            "By month"),
          YearMonthSelector(
            pr.state.month,
            onChange = ym => pr.dispatchStateChange(pr.state.copy(month = ym))
          ),
          hr(),
          label("Text search:"),
          input.text(common.formControl,
            value := pr.state.textFilter,
            onChange ==> onInputChange { textFilter =>
              pr.dispatchStateChange(pr.state.copy(textFilter = textFilter))
            })
        )
      ),
      div(grid.columnAll(10),
        Panel(Some(h3(common.panelTitle, "Transactions")),
          common.context.default,
          pr.log().renderEmpty(PreloaderC()),
          pr.log().renderPending(_ => PreloaderC()),
          pr.log().renderReady { log ⇒
            val transactions = log.collect { case t: Expense => t }.filter(pr.transactionFilter)
            val total = transactions.groupBy(_.currency).mapValues(_.map(_.amount).sum)
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
                      td(t.category.name),
                      td(amountStyle(t.amount), t.amount.toString + t.currency.sign),
                      td(t.comment.getOrElse(""): String),
                      td(textAlign.right, span(Icon.timesCircle, color := "#E74C3C", onClick --> onCancelClick(t)))
                    )
                  }
                )
              ),
              hr(),
              h4("Totals by currency: "),
              total.map { case (currency, amount) => h4(logNegAmount, renderAmount(amount) + currency.sign) },

              hr(),
              h4("Total in eur: ", span(logNegAmount, renderAmount(total.map {
                case (currency, amount) => AppCircuit.exchange(amount, currency, Currency.EUR)
              }.sum) + Currency.EUR.sign)
              )
            )
          }
        )
      )
    )
  }

  val component = ReactComponentB[Props]("LogComponent")
    .renderBackend[Backend]
    .componentDidMount(scope => scope.props.resetUIState >> scope.props.loadData)
    .componentWillReceiveProps(scope => Callback.when(scope.currentProps.shouldReload(scope.nextProps))(
      scope.nextProps.loadData
    ))
    .build
}
