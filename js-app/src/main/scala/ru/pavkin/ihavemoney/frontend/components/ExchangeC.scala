package ru.pavkin.ihavemoney.frontend.components

import java.time.Year

import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExchangeRates, Worth}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup, Icon}
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoadCategories, LoadEventLog, LoadExchangeRates}
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.ihavemoney.protocol.{CurrencyExchanged, Event, Transaction}
import ru.pavkin.utils.option._
import ReactHelpers._
import diode.ActionBatch

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Try
import scalacss.ScalaCssReact._
import diode.react.ReactPot._
import ru.pavkin.ihavemoney.frontend.components.selectors.CurrencySelector
import ru.pavkin.ihavemoney.frontend.gravatar.GravatarAPI
import ru.pavkin.utils.date._

object ExchangeC {

  case class State(
    fromAmount: String,
    fromCurrency: Currency,
    toAmount: String,
    toCurrency: Currency,
    comment: String,
    loading: Boolean = false)

  case class Props(log: ModelProxy[Pot[List[Event]]], realRates: ModelProxy[Pot[ExchangeRates]])

  class Backend($: BackendScope[Props, State]) {

    def loadData(pr: Props) = AppCircuit.dispatchCB(ActionBatch(
      LoadEventLog(Year.now),
      LoadExchangeRates()
    ))

    private def onExchangeSubmit(state: State): Callback = genSubmit(state)(api.exchange(
      BigDecimal(state.fromAmount),
      state.fromCurrency,
      BigDecimal(state.toAmount),
      state.toCurrency,
      notEmpty(state.comment)
    ))

    private def onTextChange(change: (State, String) ⇒ State)(e: ReactEventI) = {
      val newValue = e.target.value
      applyStateChange(change)(newValue)
    }

    private def amountCurrencyHelper(newAmount: String): (String, Option[Currency]) = newAmount match {
      case s if s.endsWith("e") || s.endsWith("€") ⇒ s.init → Some(Currency.EUR)
      case s if s.endsWith("$") || s.endsWith("d") ⇒ s.init → Some(Currency.USD)
      case s if s.endsWith("r") ⇒ s.init → Some(Currency.RUR)
      case _ ⇒ newAmount → None
    }

    private def applyStateChange[T](change: (State, T) ⇒ State)(newValue: T): Callback =
      $.modState(change(_, newValue))

    private def genSubmit[T](st: State)(req: ⇒ Future[T]): Callback =
      if (!isValid(st))
        Callback.alert("Invalid data")
      else
        $.modState(_.copy(loading = true)) >>
          Callback.future(req.map {
            case Left(error) ⇒ Callback.alert(s"Error: $error")
            case _ ⇒ $.modState(_.copy(
              fromAmount = "",
              toAmount = "",
              comment = "",
              fromCurrency = Currency.EUR,
              toCurrency = Currency.EUR,
              loading = false
            ))
          }) >>
          $.props.flatMap(loadData).delayMs(500).void

    private def isValid(s: State) =
      Try(BigDecimal(s.fromAmount)).isSuccess &&
        Try(BigDecimal(s.toAmount)).isSuccess &&
        s.fromCurrency != s.toCurrency

    private def renderExchangeRatePanel(clsName: String, rate: BigDecimal, from: Currency, to: Currency) =
      div(grid.columnAll(2), div(className := s"alert alert-$clsName", textAlign := "center", lineHeight := "30px",
        f"$rate%1.3f ${from.sign}/${to.sign}"))

    private def renderExchangeRate(s: State) = div(
      renderExchangeRatePanel("info", BigDecimal(s.fromAmount) / BigDecimal(s.toAmount), s.fromCurrency, s.toCurrency),
      renderExchangeRatePanel("info", BigDecimal(s.toAmount) / BigDecimal(s.fromAmount), s.toCurrency, s.fromCurrency)
    )

    private def renderRealExchangeRates(pr: Props) = pr.realRates().renderReady(rates => div(
      renderExchangeRatePanel("warning", rates.findRate(Currency.USD, Currency.RUR).getOrElse(0), Currency.USD, Currency.RUR),
      renderExchangeRatePanel("warning", rates.findRate(Currency.EUR, Currency.RUR).getOrElse(0), Currency.EUR, Currency.RUR),
      renderExchangeRatePanel("warning", rates.findRate(Currency.EUR, Currency.USD).getOrElse(0), Currency.EUR, Currency.USD)
    ))

    def render(pr: Props, state: State) = div(
      form(
        common.formHorizontal,
        onSubmit ==> dontSubmit,
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
                  Currency.values.toList,
                  style = common.context.info,
                  addAttributes = Seq(increasedFontSize, inputCurrencyAddon))
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
                  Currency.values.toList,
                  style = common.context.info,
                  addAttributes = Seq(increasedFontSize, inputCurrencyAddon))
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
        ),
        FormGroup(
          div(grid.columnAll(2), h4("Real rates")),
          renderRealExchangeRates(pr)
        )
      ),
      if (state.loading)
        PreloaderC(top := "-150px")
      else EmptyTag,

      if (!state.loading)
        pr.log().renderReady { events =>
          val conversions = events.collect {
            case e: CurrencyExchanged => e
          }
          table(className := "table table-striped table-hover table-condensed",
            thead(tr(th(""), th("Date"), th("From"), th("To"), th("Rate"), th("Comment"))),
            tbody(
              conversions.zipWithIndex.map {
                case (t, index) ⇒ tr(
                  key := index.toString,
                  td(width := "30px", paddingTop := "0px", paddingBottom := "0px", verticalAlign := "middle",
                    img(src := GravatarAPI.img(t.user, 20), className := "img-circle", title := t.user)),
                  td(t.date.ddmmyyyy),
                  td(Worth(t.fromAmount, t.fromCurrency).toPrettyString),
                  td(Worth(t.toAmount, t.toCurrency).toPrettyString),
                  td(f"${t.fromAmount / t.toAmount}%1.3f ${t.fromCurrency.sign}/${t.toCurrency.sign}, ${t.toAmount / t.fromAmount}%1.3f ${t.toCurrency.sign}/${t.fromCurrency.sign}"),
                  td(t.comment.getOrElse(""): String)
                )
              }
            )
          )
        }
      else EmptyTag
    )
  }

  val component = ReactComponentB[Props]("CurrencyExchangeComponent")
    .initialState(State("", Currency.EUR, "", Currency.RUR, ""))
    .renderBackend[Backend]
    .componentDidMount(s ⇒ s.backend.loadData(s.props))
    .build

  def apply(
    log: ModelProxy[Pot[List[Event]]],
    realRates: ModelProxy[Pot[ExchangeRates]]) = component(Props(log, realRates))
}
