package ru.pavkin.ihavemoney.frontend.bootstrap

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{ReactComponentB, ReactNode, TopNode}
import ru.pavkin.ihavemoney.frontend.bootstrap.attributes._
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scalacss.ScalaCssReact._

object Dropdown {
  case class Props(
    header: ReactNode,
    rootTag: ReactTagOf[TopNode] = div,
    buttonStyle: Option[common.context] = None,
    showCaret: Boolean = true,
    addAttributes: Seq[TagMod] = Seq())

  val component = ReactComponentB[Props]("Dropdown")
    .renderPC { (_, p, children) =>
      val ent: List[TagMod] = List(
        p.addAttributes,
        className := "dropdown-toggle",
        dataToggle := "dropdown",
        aria.haspopup := true,
        aria.expanded := false,
        p.header,
        if (p.showCaret) span(span(" "), span(common.caret)) else EmptyTag
      )

      p.rootTag(common.dropdown, common.inlineBlock,
        p.buttonStyle.map(bs => button(common.buttonOpt(bs), tpe := "button", ent))
          .getOrElse(a(ent)),
        ul(common.dropdownMenu, common.fullWidth, role := "menu", children)
      )
    }.build

  def apply[N <: TopNode](
    header: ReactNode,
    rootTag: ReactTagOf[TopNode] = div,
    buttonStyle: Option[common.context] = None,
    showCaret: Boolean = true,
    addAttributes: Seq[TagMod] = Seq())
    (children: ReactNode*) =
    component(Props(header, rootTag, buttonStyle, showCaret, addAttributes), children)
}
