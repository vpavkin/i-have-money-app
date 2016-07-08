package ru.pavkin.ihavemoney.frontend.bootstrap

import japgolly.scalajs.react.ReactNode
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scalacss.ScalaCssReact._

object FormGroup {
  def apply(children: TagMod*): ReactNode =
    div(common.formGroup, children)
}
