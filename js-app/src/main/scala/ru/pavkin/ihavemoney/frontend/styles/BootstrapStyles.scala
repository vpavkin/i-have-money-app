package ru.pavkin.ihavemoney.frontend.styles

import scalacss.Defaults._
import scalacss.mutable

class BootstrapStyles(gridSize: Int)(implicit r: mutable.Register) extends StyleSheet.Inline()(r) {

  import dsl._

  val colorPrimary = c"#375a7f"
  val colorSuccess = c"#00bc8c"
  val colorInfo = c"#3498DB"
  val colorWarning = c"#F39C12"
  val colorDanger = c"#E74C3C"

  sealed trait ClassName {self ⇒
    def name: String = self.toString
  }
  sealed trait common extends ClassName
  object common {
    case object default extends common
    case object primary extends common
    case object success extends common
    case object info extends common
    case object warning extends common
    case object danger extends common
  }

  sealed abstract class device(override val name: String) extends ClassName
  object device {
    case object extraSmall extends device("xs")
    case object small extends device("sm")
    case object medium extends device("md")
    case object large extends device("lg")
  }

  import common._
  import device._

  val commonDomain: Domain[common] = Domain.ofValues(default, primary, success, info, warning, danger)
  val contextDomain: Domain[common] = Domain.ofValues(success, info, warning, danger)
  val deviceDomain: Domain[device] = Domain.ofValues(extraSmall, small, device.medium, large)

  def commonStyle[A <: ClassName](domain: Domain[A], base: String) = styleF(domain)(opt =>
    styleS(addClassNames(base, s"$base-${opt.name}"))
  )

  def classNamesStyle(classNames: String*) = style(addClassNames(classNames: _*))

  val buttonOpt = commonStyle(commonDomain, "btn")
  val button = buttonOpt(default)
  val buttonXS = classNamesStyle("btn-xs")

  val panelOpt = commonStyle(commonDomain, "panel")
  val panel = panelOpt(default)

  val labelOpt = commonStyle(commonDomain, "label")
  val label = labelOpt(default)

  val panelHeading = classNamesStyle("panel-heading")
  val panelBody = classNamesStyle("panel-body")

  val dropdown = classNamesStyle("dropdown")
  val dropdownMenu = classNamesStyle("dropdown-menu")

  object listGroup {
    val listGroup = classNamesStyle("list-group")
    val itemOpt = commonStyle(contextDomain, "list-group-item")
    val item = classNamesStyle("list-group-item")
    val itemHeading = classNamesStyle("list-group-item-heading")
    val itemText = classNamesStyle("list-group-item-text")
  }

  val _listGroup = listGroup

  val container = classNamesStyle("container")
  val row = classNamesStyle("row")


  val table = classNamesStyle("table")
  val tableBordered = classNamesStyle("table-bordered")
  val tableStriped = classNamesStyle("table-striped")
  val tableCondensed = classNamesStyle("table-condensed")

  val navbarOpt = commonStyle(commonDomain, "navbar")
  val navbar = navbarOpt(default)
  val navbarFixedTop = classNamesStyle("navbar navbar-fixed-top")

  val navbarSection = classNamesStyle("nav", "navbar-nav")
  val navbarHeader = classNamesStyle("navbar-header")
  val navbarRight = classNamesStyle("navbar-right")
  val navbarCollapse = classNamesStyle("navbar-collapse", "collapse")
  val navbarBrand = classNamesStyle("navbar-brand")

  val formGroup = classNamesStyle("form-group")
  val formControl = classNamesStyle("form-control")

  val caret = classNamesStyle("caret")

  val sizeDomain = Domain.ofRange(1 to gridSize)
  private def columnClass(device: device, size: Int) = s"col-${device.name}-$size"
  val columnMD = styleF(sizeDomain)(size => addClassNames(columnClass(device.medium, size)))
  val columnAll = styleF(sizeDomain)(size => addClassNames(
    columnClass(device.small, size),
    columnClass(device.extraSmall, size),
    columnClass(device.medium, size),
    columnClass(device.large, size)
  ))
}
