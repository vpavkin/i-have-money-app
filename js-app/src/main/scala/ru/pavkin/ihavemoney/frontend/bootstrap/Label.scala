package ru.pavkin.ihavemoney.frontend.bootstrap

import japgolly.scalajs.react.ReactNode
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scalacss.ScalaCssReact._

object Label {
  def apply(style: common.context = common.context.default)(children: TagMod*): ReactNode =
    span(common.labelOpt(style), children)
}
