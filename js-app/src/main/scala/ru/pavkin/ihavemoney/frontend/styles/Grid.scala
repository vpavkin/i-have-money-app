package ru.pavkin.ihavemoney.frontend.styles

import ru.pavkin.ihavemoney.frontend.styles.Global.common._

import scalacss.Defaults._

class Grid(gridSize: Int = 12)(implicit r: StyleSheet.Register) extends StyleSheet.Inline()(r) {

  import dsl._

  val sizeDomain = Domain.ofRange(1 to gridSize)

  private def columnClass(device: Device, size: Int) = s"col-${device.name}-$size"
  private def columnOffsetClass(device: Device, size: Int) = s"col-${device.name}-offset-$size"

  val columnMD = styleF(sizeDomain)(size => addClassNames(columnClass(Device.medium, size)))
  val columnAll = styleF(sizeDomain)(size => addClassNames(
    columnClass(Device.small, size),
    columnClass(Device.extraSmall, size),
    columnClass(Device.medium, size),
    columnClass(Device.large, size)
  ))

  val columnOffsetMD = styleF(sizeDomain)(size => addClassNames(columnOffsetClass(Device.medium, size)))
  val columnOffsetAll = styleF(sizeDomain)(size => addClassNames(
    columnOffsetClass(Device.small, size),
    columnOffsetClass(Device.extraSmall, size),
    columnOffsetClass(Device.medium, size),
    columnOffsetClass(Device.large, size)
  ))
}
