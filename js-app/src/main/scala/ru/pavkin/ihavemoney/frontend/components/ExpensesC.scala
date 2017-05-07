package ru.pavkin.ihavemoney.frontend.components

import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExpenseCategory}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup}
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.utils.option._
import helpers._
import ru.pavkin.ihavemoney.frontend.components.selectors.ExpenseCategorySelector
import ru.pavkin.utils.date.LocalDateParser

import scalacss.ScalaCssReact._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object ExpensesC extends CommonTransactionC[ExpenseCategory] {

  val defaultCategories = Set(
    "Groceries", "Lunches", "Entertainment", "Communication", "Presents", "Traveling", "Transport",
    "Creativity", "Sport", "Health", "Beauty", "Clothes", "Rent & Utility", "Fees & Taxes", "Other", "Education"
  ).map(ExpenseCategory(_))

  class Backend($: BackendScope[Props, State]) extends CommonTransactionBackend($) {

    def onFormSubmit(state: State): Callback = genSubmit(state)(api.addExpense(
      BigDecimal(state.amount),
      state.currency,
      state.category,
      LocalDateParser.fromYYYYMMDD(state.transactionDate).get,
      initializer = state.initializer,
      notEmpty(state.comment)
    ))

    def renderSubmitButton(pr: Props, state: State): ReactElement =
      Button(Callback.empty,
        style = common.context.danger,
        addAttributes = Seq(`type` := "submit", disabled := (!isValid(state) || state.loading)),
        addStyles = Seq(increasedFontSize)
      )("Add Expense")

    def renderCategoriesSelector(pr: Props, state: State): Categories ⇒ ReactElement = categories ⇒
      ExpenseCategorySelector(
        state.category,
        s ⇒ applyStateChange[ExpenseCategory]((st, v) ⇒ st.copy(category = v))(s),
        enrich(categories.expense).toList,
        style = common.context.danger,
        addAttributes = Seq(increasedFontSize))

    def render(pr: Props, state: State) = renderForm(pr, state)

    def onDateChange(e: ReactEventI) =
      e.extract(_.target.value)(text => $.modState(s => s.copy(
        transactionDate = text
      )))

    override def renderAdditionalFields(pr: Props, state: State): ReactNode =
      FormGroup(common.hasErrorOpt(!isValidDate(state.transactionDate)),
        HorizontalForm.Label("Date"),
        div(HorizontalForm.input, input.date(value := state.transactionDate, common.formControl, increasedFontSize,
          onChange ==> onDateChange
        ))
      )
  }

  val component = ReactComponentB[Props]("AddExpensesComponent")
    .initialState(State(Currency.EUR, "", ExpenseCategory("Groceries"), ""))
    .renderBackend[Backend]
    .componentDidMount(s ⇒ s.backend.init)
    .build

  def apply(categories: ModelProxy[Pot[Categories]]) = component(Props(categories))
}
