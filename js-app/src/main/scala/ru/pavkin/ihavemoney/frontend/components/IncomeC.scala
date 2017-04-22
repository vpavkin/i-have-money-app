package ru.pavkin.ihavemoney.frontend.components

import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, IncomeCategory}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.Button
import ru.pavkin.ihavemoney.frontend.components.selectors.IncomeCategorySelector
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.utils.option._

import scalacss.ScalaCssReact._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object IncomeC extends CommonTransactionC[IncomeCategory] {

  val defaultCategories = Set("Salary", "Presents", "Sale", "Assets").map(IncomeCategory(_))

  class Backend($: BackendScope[Props, State]) extends CommonTransactionBackend($) {

    def onIncomeSubmit(state: State) = genSubmit(state)(api.addIncome(
      BigDecimal(state.amount),
      state.currency,
      state.category,
      initializer = state.initializer,
      notEmpty(state.comment)
    ))

    def renderSubmitButton(pr: Props, state: State): ReactElement =
      Button(onIncomeSubmit(state),
        style = common.context.success,
        addAttributes = Seq(disabled := (!isValid(state) || state.loading)),
        addStyles = Seq(increasedFontSize)
      )("Add Income")

    def renderCategoriesSelector(pr: Props, state: State): Categories ⇒ ReactElement = categories ⇒
      IncomeCategorySelector(
        state.category,
        s ⇒ applyStateChange[IncomeCategory]((st, v) ⇒ st.copy(category = v))(s),
        enrich(categories.income).toList,
        style = common.context.success,
        addAttributes = Seq(increasedFontSize))

    def render(pr: Props, state: State) = renderForm(pr, state)
  }

  val component = ReactComponentB[Props]("IncomeComponent")
    .initialState(State(Currency.EUR, "", IncomeCategory("Salary"), ""))
    .renderBackend[Backend]
    .componentDidMount(s ⇒ s.backend.init)
    .build

  def apply(categories: ModelProxy[Pot[Categories]]) = component(Props(categories))
}
