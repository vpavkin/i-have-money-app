package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.{BackendScope, Callback, ReactEventI}

trait ReactHelpers {

  def onInputChange[State](scope: BackendScope[_, State])(upd: (State, String) => State)(e: ReactEventI): Callback =
    e.extract(_.target.value)(text => scope.modState(s => upd(s, text)))

  def onInputChange(cb: String => Callback)(e: ReactEventI): Callback =
    e.extract(_.target.value)(text => cb(text))

  def dontSubmit(e: ReactEventI): Callback = e.preventDefaultCB

}

object ReactHelpers extends ReactHelpers
