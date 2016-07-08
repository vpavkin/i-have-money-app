package ru.pavkin.ihavemoney.frontend.components

import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.Button
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.utils.option._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object ExpensesC extends CommonTransactionC {

  val defaultCategories = Set("Groceries", "Lunches", "Entertainment", "Communication", "Presents", "Traveling", "Transport", "Creativity", "Sport", "Health", "Beauty", "Clothes", "Rent & Utility", "Fees & Taxes", "Other", "Education")

  class Backend($: BackendScope[Props, State]) extends CommonTransactionBackend($) {

    def onExpenseSubmit(state: State): Callback = genSubmit(state)(api.addExpense(
      BigDecimal(state.amount),
      Currency.unsafeFromCode(state.currency),
      state.category,
      initializer = state.initializer,
      notEmpty(state.comment)
    ))

    def renderSubmitButton(pr: Props, state: State): ReactElement =
      Button(onExpenseSubmit(state),
        style = common.context.danger,
        addAttributes = Seq(disabled := (!isValid(state)))
      )("Add Expense")

    def renderCategoriesSelector(pr: Props, state: State): Categories ⇒ ReactElement = categories ⇒
      StringValueSelector(
        state.category,
        s ⇒ applyStateChange((st, v) ⇒ st.copy(category = v))(s),
        enrich(categories.expense).toList,
        addStyles = Seq(increasedFontSize))

    def render(pr: Props, state: State) = renderForm(pr, state)
  }

  val component = ReactComponentB[Props]("AddExpensesComponent")
      .initialState(State("EUR", "", "Groceries", ""))
      .renderBackend[Backend]
      .componentDidMount(s ⇒ s.backend.init)
      .build

  def apply(categories: ModelProxy[Pot[Categories]]) = component(Props(categories))
}
