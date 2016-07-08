package ru.pavkin.ihavemoney.frontend.components

import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.gravatar.GravatarAPI
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LoadTransactionLog
import ru.pavkin.ihavemoney.protocol.Transaction
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.utils.date._

import scalacss.ScalaCssReact._

object TransactionLogC {

  case class Props(log: ModelProxy[Pot[List[Transaction]]])

  class Backend($: BackendScope[Props, Unit]) {

    def loadTransactionLog(pr: Props) = Callback {
      AppCircuit.dispatch(LoadTransactionLog())
    }

    def amountStyle(amount: BigDecimal) =
      if (amount >= 0) logPosAmount
      else logNegAmount

    def render(pr: Props) = {
      div(
        pr.log().renderEmpty("Loading..."),
        pr.log().renderPending(_ => div("Loading...")),
        pr.log().renderReady(log ⇒
          div(
            table(className := "table table-striped table-hover table-condensed",
              thead(tr(th(""), th("Date"), th("Category"), th("Amount"), th("Comment"))),
              tbody(
                log.map(t ⇒ tr(
                  td(width := "30px", paddingTop := "0px", paddingBottom := "0px", verticalAlign := "middle",
                    img(src := GravatarAPI.img(t.user, 20), className := "img-circle", title := t.user)),
                  td(t.date.ddmmyyyy),
                  td(t.category),
                  td(amountStyle(t.amount), t.amount.toString + t.currency.sign),
                  td(t.comment.getOrElse(""): String)
                ))
              )
            )
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("LogComponent")
      .renderBackend[Backend]
      .componentDidMount(s ⇒ s.backend.loadTransactionLog(s.props))
      .build
}
