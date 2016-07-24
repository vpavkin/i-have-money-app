package ru.pavkin.ihavemoney.frontend.components

import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup}
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.utils.option._
import helpers._
import ru.pavkin.utils.date.LocalDateParser

import scalacss.ScalaCssReact._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object ExpensesC extends CommonTransactionC {

  val defaultCategories = Set("Groceries", "Lunches", "Entertainment", "Communication", "Presents", "Traveling", "Transport", "Creativity", "Sport", "Health", "Beauty", "Clothes", "Rent & Utility", "Fees & Taxes", "Other", "Education")

  class Backend($: BackendScope[Props, State]) extends CommonTransactionBackend($) {

    def onExpenseSubmit(state: State): Callback = genSubmit(state)(api.addExpense(
      BigDecimal(state.amount),
      state.currency,
      state.category,
      LocalDateParser.fromYYYYMMDD(state.transactionDate).get,
      initializer = state.initializer,
      notEmpty(state.comment)
    ))

    def renderSubmitButton(pr: Props, state: State): ReactElement =
      Button(onExpenseSubmit(state),
        style = common.context.danger,
        addAttributes = Seq(disabled := (!isValid(state) || state.loading)),
        addStyles = Seq(increasedFontSize)
      )("Add Expense")

    def renderCategoriesSelector(pr: Props, state: State): Categories ⇒ ReactElement = categories ⇒
      div(StringValueSelector(
        state.category,
        s ⇒ applyStateChange[String]((st, v) ⇒ st.copy(category = v))(s),
        enrich(categories.expense).toList,
        addStyles = Seq(increasedFontSize))
      )

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
      .initialState(State(Currency.EUR, "", "Groceries", ""))
      .renderBackend[Backend]
      .componentDidMount(s ⇒ s.backend.init)
      .build

  def apply(categories: ModelProxy[Pot[Categories]]) = component(Props(categories))
}
