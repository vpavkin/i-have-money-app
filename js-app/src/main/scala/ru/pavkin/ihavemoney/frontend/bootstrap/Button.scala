package ru.pavkin.ihavemoney.frontend.bootstrap

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{Callback, ReactComponentB, ReactNode}
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scalacss.ScalaCssReact._
import scalacss.StyleA

object Button {

  case class Props(onClick: Callback = Callback.empty,
                   style: common.context = common.context.default,
                   addAttributes: Seq[TagMod] = Seq(),
                   addStyles: Seq[StyleA] = Seq())

  val component = ReactComponentB[Props]("Button")
    .renderPC((_, p, c) =>
      button(common.buttonOpt(p.style), p.addStyles, tpe := "button", onClick --> p.onClick, p.addAttributes, c)
    ).build

  def apply(onClick: Callback = Callback.empty,
            style: common.context = common.context.default,
            addAttributes: Seq[TagMod] = Seq(),
            addStyles: Seq[StyleA] = Seq())
           (children: ReactNode*) = component(Props(onClick, style, addAttributes, addStyles), children)

  def close(onClickCallback: Callback = Callback.empty) = button(tpe := "button", common.close, onClick --> onClickCallback, Icon.close)
}
