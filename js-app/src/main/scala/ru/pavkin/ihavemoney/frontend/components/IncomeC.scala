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

object IncomeC extends CommonTransactionC {

  val defaultCategories = Set("Salary", "Presents", "Sale", "Assets")

  class Backend($: BackendScope[Props, State]) extends CommonTransactionBackend($) {

    def onIncomeSubmit(state: State) = genSubmit(state)(api.addIncome(
      BigDecimal(state.amount),
      Currency.unsafeFromCode(state.currency),
      state.category,
      initializer = false,
      notEmpty(state.comment)
    ))

    def renderSubmitButton(pr: Props, state: State): ReactElement =
      Button(onIncomeSubmit(state),
        style = common.context.success,
        addAttributes = Seq(disabled := (!isValid(state)))
      )("Add Income")

    def renderCategoriesSelector(pr: Props, state: State): Categories ⇒ ReactElement = categories ⇒
      StringValueSelector(
        state.category,
        s ⇒ applyStateChange((st, v) ⇒ st.copy(category = v))(s),
        enrich(categories.income).toList,
        contextStyle = common.context.success,
        addStyles = Seq(increasedFontSize))

    def render(pr: Props, state: State) = renderForm(pr, state)
  }

  val component = ReactComponentB[Props]("IncomeComponent")
      .initialState(State("EUR", "", defaultCategories.toList.sorted.head, ""))
      .renderBackend[Backend]
      .componentDidMount(s ⇒ s.backend.init)
      .build

  def apply(categories: ModelProxy[Pot[Categories]]) = component(Props(categories))
}
