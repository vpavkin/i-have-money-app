package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react.Callback

trait RemoteDataProps[T <: RemoteDataProps[T]] {self =>
  def loadData: Callback
  def shouldReload(nextProps: T): Boolean
}
