package ru.pavkin.ihavemoney.frontend.components

import cats.data.Xor
import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{Callback, _}
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, Checkbox, FormGroup}
import ru.pavkin.ihavemoney.frontend.bootstrap.attributes._
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadCategories
import ru.pavkin.ihavemoney.frontend.redux.model.Categories
import ru.pavkin.ihavemoney.frontend.styles.Global.{style ⇒ _, _}

import scalacss.ScalaCssReact._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import diode.react.ReactPot._

abstract class CommonTransactionC(implicit ec: ExecutionContext) {

  case class State(
      currency: Currency,
      amount: String,
      category: String,
      comment: String,
      initializer: Boolean = false,
      loading: Boolean = false)

  case class Props(categories: ModelProxy[Pot[Categories]])

  def defaultCategories: Set[String]
  def enrich(real: List[String]): Set[String] = defaultCategories ++ real

  abstract class CommonTransactionBackend($: BackendScope[Props, State]) {
    def onTextChange(change: (State, String) ⇒ State)(e: ReactEventI) = {
      val newValue = e.target.value
      applyStateChange(change)(newValue)
    }

    def amountCurrencyHelper(oldState: State, newAmount: String): State = newAmount match {
      case s if s.endsWith("e") || s.endsWith("€") ⇒ oldState.copy(Currency.EUR, s.init)
      case s if s.endsWith("$") || s.endsWith("d") ⇒ oldState.copy(Currency.USD, s.init)
      case s if s.endsWith("r") ⇒ oldState.copy(Currency.RUR, s.init)
      case _ ⇒ oldState.copy(amount = newAmount)
    }

    def applyStateChange[T](change: (State, T) ⇒ State)(newValue: T): Callback =
      $.modState(change(_, newValue))

    def onFormSubmit(e: ReactEventI) = e.preventDefaultCB

    def genSubmit[T](st: State)(req: ⇒ Future[T]): Callback =
      if (!isValid(st))
        Callback.alert("Invalid data")
      else
        $.modState(_.copy(loading = true)) >>
            Callback.future(req.map {
              case Xor.Left(error) ⇒ Callback.alert(s"Error: $error")
              case _ ⇒ $.modState(_.copy(loading = false))
            })


    def isValid(s: State) =
      Try(BigDecimal(s.amount)).isSuccess &&
          s.category.nonEmpty

    def init: Callback = Callback {
      AppCircuit.dispatch(LoadCategories())
    }

    def renderSubmitButton(pr: Props, state: State): ReactElement
    def renderCategoriesSelector(pr: Props, state: State): Categories ⇒ ReactElement

    def renderForm(pr: Props, state: State) = div(
      form(
        common.formHorizontal,
        onSubmit ==> onFormSubmit,
        FormGroup(
          HorizontalForm.Label("Amount", "amountInput"),
          div(grid.columnAll(8),
            div(className := "input-group",
              input.text(
                id := "amountInput",
                required := true,
                common.formControl,
                addonMainInput,
                increasedFontSize, rightMargin,
                placeholder := "Amount",
                value := state.amount,
                onChange ==> onTextChange((s, v) ⇒ amountCurrencyHelper(s, v))
              ),
              div(className := "input-group-btn",
                CurrencySelector(
                  state.currency,
                  c ⇒ applyStateChange[Currency]((st, v) ⇒ st.copy(currency = v))(c),
                  addStyles = Seq(increasedFontSize, inputCurrencyAddon))
              )
            )
          ),
          pr.categories().renderReady(categories ⇒
            renderCategoriesSelector(pr, state)(categories)
          )
        ),
        FormGroup(
          HorizontalForm.Label("Comment", "commentInput"),
          div(HorizontalForm.input, input.text(
            common.formControl,
            id := "commentInput",
            increasedFontSize, addonMainInput,
            placeholder := "Comment",
            value := state.comment,
            onChange ==> onTextChange((s, v) ⇒ s.copy(comment = v))
          ))
        ),
        if (AppCircuit.fortune.initializationMode)
          FormGroup(div(grid.columnOffsetAll(HorizontalForm.LABEL_WIDTH), HorizontalForm.input,
            Checkbox(isChecked ⇒ $.modState(_.copy(initializer = isChecked)), state.initializer,
              dataToggle := "tooltip",
              increasedFontSize,
              dataPlacement := "right",
              title := "Initializer transactions are for initial fortune setup. They don't appear in transaction log and statistics.",
              "Initializer")
          ))
        else EmptyTag,
        FormGroup(
          div(grid.columnOffsetAll(HorizontalForm.LABEL_WIDTH), grid.columnAll(10),
            renderSubmitButton(pr, state)
          )
        )
      ),
      if (state.loading)
        PreloaderC(top := "-150px")
      else EmptyTag
    )
  }

}
