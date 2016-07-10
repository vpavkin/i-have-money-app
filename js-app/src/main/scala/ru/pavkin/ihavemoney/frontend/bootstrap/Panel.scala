package ru.pavkin.ihavemoney.frontend.bootstrap

import japgolly.scalajs.react.{ReactElement, ReactNode}
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scalacss.ScalaCssReact._

object Panel {
  def apply(heading: Option[ReactElement], style: common.context, children: TagMod*): ReactNode =
    div(common.panelOpt(style),
      heading.map(el ⇒ div(common.panelHeading, heading)).getOrElse(EmptyTag),
      div(common.panelBody, children)
    )
}
