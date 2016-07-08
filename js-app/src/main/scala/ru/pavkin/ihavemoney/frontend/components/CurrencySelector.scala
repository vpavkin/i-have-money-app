package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{ReactComponentB, _}
import ru.pavkin.ihavemoney.domain.fortune.Currency
import ru.pavkin.ihavemoney.frontend.bootstrap.Dropdown
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scalacss.StyleA


object CurrencySelector {

  case class Props(
      selected: Currency,
      onChange: Currency ⇒ Callback,
      contextStyle: common.context = common.context.info,
      addStyles: Seq[StyleA] = Seq())

  def renderElement(pr: Props)(m: Currency) =
    li(key := m.code, a(onClick --> pr.onChange(m), m.sign))

  val component = ReactComponentB[Props]("StringValueSelector")
      .renderPC((_, p, c) =>
        Dropdown(p.selected.sign, a, Some(p.contextStyle), addStyles = p.addStyles)(
          Currency.values.map(renderElement(p))
        )
      ).build

  def apply(
      selected: Currency,
      onChange: Currency ⇒ Callback,
      contextStyle: common.context = common.context.info,
      addStyles: Seq[StyleA] = Seq()) = component(Props(selected, onChange, contextStyle, addStyles))
}
