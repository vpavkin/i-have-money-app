package ru.pavkin.ihavemoney.frontend.components

import cats.data.Xor
import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{Callback, _}
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup}
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
      currency: String,
      amount: String,
      category: String,
      comment: String)

  case class Props(categories: ModelProxy[Pot[Categories]])

  def defaultCategories: Set[String]
  def enrich(real: List[String]): Set[String] = defaultCategories ++ real

  abstract class CommonTransactionBackend($: BackendScope[Props, State]) {
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

    def isValid(s: State) =
      Try(BigDecimal(s.amount)).isSuccess &&
          Currency.isCurrency(s.currency) &&
          s.category.nonEmpty

    def init: Callback = Callback {
      AppCircuit.dispatch(LoadCategories())
    }

    def renderSubmitButton(pr: Props, state: State): ReactElement
    def renderCategoriesSelector(pr: Props, state: State): Categories ⇒ ReactElement

    def renderForm(pr: Props, state: State) =
      form(
        common.formHorizontal,
        onSubmit ==> onFormSubmit,
        FormGroup(
          HorizontalForm.Label("Amount", "amountInput"),
          div(grid.columnAll(2), input.text(
            id := "amountInput",
            required := true,
            common.formControl,
            placeholder := "Amount",
            value := state.amount,
            onChange ==> onTextChange((s, v) ⇒ s.copy(amount = v))
          )),
          div(grid.columnAll(1), select(
            required := true,
            common.formControl,
            id := "currencyInput",
            value := state.currency,
            onChange ==> onTextChange((s, v) ⇒ s.copy(currency = v)),
            List("USD", "EUR", "RUR").map(option(_))
          )),
          pr.categories().renderReady(categories ⇒
            div(grid.columnAll(2), renderCategoriesSelector(pr, state)(categories))
          )
        ),
        FormGroup(
          HorizontalForm.Label("Comment", "commentInput"),
          div(HorizontalForm.input, input.text(
            common.formControl,
            id := "commentInput",
            placeholder := "Comment",
            value := state.comment,
            onChange ==> onTextChange((s, v) ⇒ s.copy(comment = v))
          ))
        ),
        FormGroup(
          div(grid.columnOffsetAll(2), grid.columnAll(10),
            renderSubmitButton(pr, state)
          )
        )
      )
  }

}
