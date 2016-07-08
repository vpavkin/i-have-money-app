package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.{ReactComponentB, _}
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.bootstrap.Dropdown
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scalacss.StyleA


object StringValueSelector {

  case class Props(
      selected: String,
      onChange: String ⇒ Callback,
      elements: List[String],
      contextStyle: common.context = common.context.danger,
      addStyles: Seq[StyleA] = Seq())

  def renderElement(pr: Props)(m: String) =
    li(key := m, a(onClick --> pr.onChange(m), m))

  val component = ReactComponentB[Props]("StringValueSelector")
      .renderPC((_, p, c) =>
        Dropdown(p.selected, a, Some(p.contextStyle), addStyles = p.addStyles)(
          p.elements.sorted.map(renderElement(p))
        )
      ).build

  def apply(
      selected: String,
      onChange: String ⇒ Callback,
      elements: List[String],
      contextStyle: common.context = common.context.danger,
      addStyles: Seq[StyleA] = Seq()) = component(Props(selected, onChange, elements, contextStyle, addStyles))
}
