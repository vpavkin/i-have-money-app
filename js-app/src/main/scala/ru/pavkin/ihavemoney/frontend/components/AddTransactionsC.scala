package ru.pavkin.ihavemoney.frontend.components

import cats.data.Xor
import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.raw.HTMLInputElement
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup}
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadCategories
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.utils.option._
import diode.react.ReactPot._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Try
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scala.concurrent.Future
import scalacss.ScalaCssReact._

object AddTransactionsC {

  case class State(
      currency: String,
      amount: String,
      category: String,
      comment: String)

  case class Props(categories: ModelProxy[Pot[Categories]])

  val defaultCategories = Set("Salary", "Presents")

  def enrich(real: List[String]): Set[String] = defaultCategories ++ real

  class Backend($: BackendScope[Props, State]) {

    val amountInput = Ref[HTMLInputElement]("amountInput")
    val currencyInput = Ref[HTMLInputElement]("currencyInput")
    val categoryInput = Ref[HTMLInputElement]("categoryInput")
    val commentInput = Ref[HTMLInputElement]("commentInput")

    def onTextChange(change: (State, String) ⇒ State)(e: ReactEventI) = {
      val newValue = e.target.value
      applyStateChange(change)(newValue)
    }

    def applyStateChange(change: (State, String) ⇒ State)(newValue: String): Callback =
      $.modState(change(_, newValue))

    def onFormSubmit(e: ReactEventI) = e.preventDefaultCB

    def genSubmit[T](st: State)(req: ⇒ Future[T]): Callback =
      if (!isValid(st))
        Callback.alert("Invalid data")
      else Callback.future(req.map {
        case Xor.Left(error) ⇒ Callback.alert(s"Error: $error")
        case _ ⇒ Callback.alert(s"Success")
      })

    def onIncomeSubmit(state: State) = genSubmit(state)(api.addIncome(
      BigDecimal(state.amount),
      Currency.unsafeFromCode(state.currency),
      state.category,
      initializer = false,
      notEmpty(state.comment)
    ))

    def onExpenseSubmit(state: State) = genSubmit(state)(api.addExpense(
      BigDecimal(state.amount),
      Currency.unsafeFromCode(state.currency),
      state.category,
      initializer = false,
      notEmpty(state.comment)
    ))

    def isValid(s: State) =
      Try(BigDecimal(s.amount)).isSuccess &&
          Currency.isCurrency(s.currency) &&
          s.category.nonEmpty

    def init: Callback = Callback {
      AppCircuit.dispatch(LoadCategories())
    }

    def render(pr: Props, state: State) = {
      val valid = isValid(state)
      form(
        common.formHorizontal,
        onSubmit ==> onFormSubmit,
        FormGroup(
          HorizontalForm.Label("Amount", "amountInput"),
          div(grid.columnAll(8), input.number(
            min := 0.0,
            step := 0.01,
            ref := amountInput,
            id := "amountInput",
            required := true,
            common.formControl,
            placeholder := "Amount",
            value := state.amount,
            onChange ==> onTextChange((s, v) ⇒ s.copy(amount = v))
          )),
          div(grid.columnAll(2), select(
            ref := currencyInput,
            required := true,
            common.formControl,
            id := "currencyInput",
            value := state.currency,
            onChange ==> onTextChange((s, v) ⇒ s.copy(currency = v)),
            List("USD", "EUR", "RUR").map(option(_))
          ))
        ),
        pr.categories().renderReady(categories ⇒
          FormGroup(
            HorizontalForm.Label("Category", "categoryInput"),
            div(HorizontalForm.input, StringValueSelector(
              state.category,
              s ⇒ applyStateChange((st, v) ⇒ st.copy(category = v))(s),
              enrich(categories.income ++ categories.expense).toList,
              addStyles = Seq(increasedFontSize)))
          )
        ),
        FormGroup(
          HorizontalForm.Label("Comment", "commentInput"),
          div(HorizontalForm.input, input.text(
            ref := commentInput,
            common.formControl,
            id := "commentInput",
            placeholder := "Comment",
            value := state.comment,
            onChange ==> onTextChange((s, v) ⇒ s.copy(comment = v))
          ))
        ),
        FormGroup(
          div(grid.columnOffsetAll(2), grid.columnAll(10),
            Button(onIncomeSubmit(state),
              style = common.context.success,
              addStyles = Seq(rightMargin),
              addAttributes = Seq(disabled := (!valid))
            )("Income"),
            Button(onExpenseSubmit(state),
              style = common.context.danger,
              addAttributes = Seq(disabled := (!valid))
            )("Expense")
          )
        )
      )
    }
  }
  val component = ReactComponentB[Props]("AddTransactionsComponent")
      .initialState(State("USD", "1000", defaultCategories.toList.sorted.head, ""))
      .renderBackend[Backend]
      .componentDidMount(s ⇒ s.backend.init)
      .build

  def apply(categories: ModelProxy[Pot[Categories]]) = component(Props(categories))
}
