package ru.pavkin.ihavemoney.frontend.components

import cats.data.Xor
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup, Icon}
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.utils.option._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Try
import scalacss.ScalaCssReact._

object ExchangeC {

  case class State(
      fromAmount: String,
      fromCurrency: Currency,
      toAmount: String,
      toCurrency: Currency,
      comment: String)

  case class Props()

  class Backend($: BackendScope[Props, State]) {

    def onExchangeSubmit(state: State): Callback = genSubmit(state)(api.exchange(
      BigDecimal(state.fromAmount),
      state.fromCurrency,
      BigDecimal(state.toAmount),
      state.toCurrency,
      notEmpty(state.comment)
    ))

    def onTextChange(change: (State, String) ⇒ State)(e: ReactEventI) = {
      val newValue = e.target.value
      applyStateChange(change)(newValue)
    }

    def amountCurrencyHelper(newAmount: String): (String, Option[Currency]) = newAmount match {
      case s if s.endsWith("e") || s.endsWith("€") ⇒ s.init → Some(Currency.EUR)
      case s if s.endsWith("$") || s.endsWith("d") ⇒ s.init → Some(Currency.USD)
      case s if s.endsWith("r") ⇒ s.init → Some(Currency.RUR)
      case _ ⇒ newAmount → None
    }

    def applyStateChange[T](change: (State, T) ⇒ State)(newValue: T): Callback =
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
      Try(BigDecimal(s.fromAmount)).isSuccess &&
          Try(BigDecimal(s.toAmount)).isSuccess &&
          s.fromCurrency != s.toCurrency

    def renderExchangeRate(s: State) = div(
      div(grid.columnAll(2), div(className := "alert alert-info", textAlign := "center", lineHeight := "30px",
        f"${BigDecimal(s.fromAmount) / BigDecimal(s.toAmount)}%1.3f ${s.fromCurrency.sign}/${s.toCurrency.sign}")),
      div(grid.columnAll(2), div(className := "alert alert-info", textAlign := "center", lineHeight := "30px",
        f"${BigDecimal(s.toAmount) / BigDecimal(s.fromAmount)}%1.3f ${s.toCurrency.sign}/${s.fromCurrency.sign}"))
    )

    def render(pr: Props, state: State) =
      form(
        common.formHorizontal,
        onSubmit ==> onFormSubmit,
        FormGroup(
          div(grid.columnAll(5),
            div(className := "input-group",
              input.text(
                required := true,
                common.formControl,
                addonMainInput,
                increasedFontSize, rightMargin,
                placeholder := "From",
                value := state.fromAmount,
                onChange ==> onTextChange { (s, v) ⇒
                  val (am, copt) = amountCurrencyHelper(v)
                  copt.map(c ⇒ s.copy(fromCurrency = c))
                      .getOrElse(s).copy(fromAmount = am)
                }
              ),
              div(className := "input-group-btn",
                CurrencySelector(
                  state.fromCurrency,
                  c ⇒ applyStateChange[Currency]((st, v) ⇒ st.copy(fromCurrency = v))(c),
                  addStyles = Seq(increasedFontSize, inputCurrencyAddon))
              )
            )
          ),
          div(grid.columnAll(1), fontSize := "48px", lineHeight := "48px", textAlign := "center", Icon.arrowCircleORight),
          div(grid.columnAll(5),
            div(className := "input-group",
              input.text(
                required := true,
                common.formControl,
                addonMainInput,
                increasedFontSize, rightMargin,
                placeholder := "To",
                value := state.toAmount,
                onChange ==> onTextChange { (s, v) ⇒
                  val (am, copt) = amountCurrencyHelper(v)
                  copt.map(c ⇒ s.copy(toCurrency = c))
                      .getOrElse(s).copy(toAmount = am)
                }
              ),
              div(className := "input-group-btn",
                CurrencySelector(
                  state.toCurrency,
                  c ⇒ applyStateChange[Currency]((st, v) ⇒ st.copy(toCurrency = v))(c),
                  addStyles = Seq(increasedFontSize, inputCurrencyAddon))
              )
            )
          )
        ),
        FormGroup(
          div(grid.columnAll(11), input.text(
            common.formControl,
            id := "commentInput",
            increasedFontSize, addonMainInput,
            placeholder := "Comment",
            value := state.comment,
            onChange ==> onTextChange((s, v) ⇒ s.copy(comment = v))
          ))
        ),
        FormGroup(
          div(grid.columnAll(2),
            Button(onExchangeSubmit(state),
              style = common.context.success,
              addAttributes = Seq(disabled := (!isValid(state))),
              addStyles = Seq(increasedFontSize, common.buttonLarge)
            )("Exchange")
          ),
          if (isValid(state))
            renderExchangeRate(state)
          else EmptyTag
        )
      )
  }

  val component = ReactComponentB[Props]("CurrencyExchangeComponent")
      .initialState(State("", Currency.EUR, "", Currency.RUR, ""))
      .renderBackend[Backend]
      .build

  def apply() = component(Props())
}
