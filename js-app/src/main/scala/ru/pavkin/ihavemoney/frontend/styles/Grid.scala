package ru.pavkin.ihavemoney.frontend.styles

import ru.pavkin.ihavemoney.frontend.styles.Global.common._

import scalacss.mutable
import scalacss.mutable.StyleSheet

class Grid(gridSize: Int = 12)(implicit r: mutable.Register) extends StyleSheet.Inline()(r) {

  import dsl._

  val sizeDomain = Domain.ofRange(1 to gridSize)

  private def columnClass(device: device, size: Int) = s"col-${device.name}-$size"
  private def columnOffsetClass(device: device, size: Int) = s"col-${device.name}-offset-$size"

  val columnMD = styleF(sizeDomain)(size => addClassNames(columnClass(device.medium, size)))
  val columnAll = styleF(sizeDomain)(size => addClassNames(
    columnClass(device.small, size),
    columnClass(device.extraSmall, size),
    columnClass(device.medium, size),
    columnClass(device.large, size)
  ))

  val columnOffsetMD = styleF(sizeDomain)(size => addClassNames(columnOffsetClass(device.medium, size)))
  val columnOffsetAll = styleF(sizeDomain)(size => addClassNames(
    columnOffsetClass(device.small, size),
    columnOffsetClass(device.extraSmall, size),
    columnOffsetClass(device.medium, size),
    columnOffsetClass(device.large, size)
  ))
}
