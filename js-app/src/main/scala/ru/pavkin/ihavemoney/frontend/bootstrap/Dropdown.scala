package ru.pavkin.ihavemoney.frontend.bootstrap

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{ReactComponentB, ReactNode, TopNode}
import ru.pavkin.ihavemoney.frontend.bootstrap.attributes._
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scalacss.ScalaCssReact._
import scalacss.StyleA

object Dropdown {
  case class Props(header: ReactNode,
                   rootTag: ReactTagOf[TopNode] = div,
                   buttonStyle: Option[common.context] = None,
                   showCaret: Boolean = true,
                   addStyles: Seq[StyleA] = Seq()) {
    def isButton = buttonStyle.isDefined
  }

  val component = ReactComponentB[Props]("Dropdown")
    .renderPC { (_, p, children) ⇒
      val ent: List[TagMod] = List(
        p.addStyles,
        className := "dropdown-toggle", dataToggle := "dropdown", role := "button", aria.haspopup := true, aria.expanded := false,
        p.header,
        if (p.showCaret) span(span(" "), span(common.caret)) else EmptyTag
      )

      p.rootTag(common.dropdown,
        if (p.isButton) button(common.buttonOpt(p.buttonStyle.get), tpe := "button", ent)
        else a(ent),
        ul(common.dropdownMenu, children)
      )
    }.build

  def apply[N <: TopNode](header: ReactNode,
                          rootTag: ReactTagOf[TopNode] = div,
                          buttonStyle: Option[common.context] = None,
                          showCaret: Boolean = true,
                          addStyles: Seq[StyleA] = Seq())
                         (children: ReactNode*) = component(Props(header, rootTag, buttonStyle, showCaret, addStyles), children)
}
