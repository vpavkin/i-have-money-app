package ru.pavkin.ihavemoney.frontend.styles

import scala.language.postfixOps
import scalacss.Defaults._
import scalacss.{StyleS, mutable}

class Base(implicit r: mutable.Register) extends StyleSheet.Inline()(r) {

  import dsl._

  val colorPrimary = c"#375a7f"
  val colorSuccess = c"#00bc8c"
  val colorInfo = c"#3498DB"
  val colorWarning = c"#F39C12"
  val colorDanger = c"#E74C3C"

  sealed trait ClassName {self ⇒
    def name: String = self.toString
  }
  sealed trait context extends ClassName
  object context {
    case object default extends context
    case object primary extends context
    case object success extends context
    case object info extends context
    case object warning extends context
    case object danger extends context
  }

  sealed abstract class device(override val name: String) extends ClassName
  object device {
    case object extraSmall extends device("xs")
    case object small extends device("sm")
    case object medium extends device("md")
    case object large extends device("lg")
  }

  import context._
  import device._

  val commonDomain: Domain[context] = Domain.ofValues(default, primary, success, info, warning, danger)
  val contextDomain: Domain[context] = Domain.ofValues(success, info, warning, danger)
  val deviceDomain: Domain[device] = Domain.ofValues(extraSmall, small, device.medium, large)

  def commonStyle[A <: ClassName](domain: Domain[A], base: String) = styleF(domain)(opt =>
    styleS(addClassNames(base, s"$base-${opt.name}"))
  )

  def classNamesStyle(classNames: String*) = style(addClassNames(classNames: _*))

  val hasErrorOpt = styleF(Domain.boolean)(b ⇒ if (b) addClassName("has-error") else StyleS.empty)

  val buttonOpt = commonStyle(commonDomain, "btn")
  val button = buttonOpt(default)
  val buttonXS = classNamesStyle("btn-xs")
  val buttonLarge = classNamesStyle("btn-lg")
  val close = classNamesStyle("close")

  val panelOpt = commonStyle(commonDomain, "panel")
  val panel = panelOpt(default)

  val labelOpt = commonStyle(commonDomain, "label")
  val label = labelOpt(default)

  val alertOpt = commonStyle(commonDomain, "alert")
  val alert = alertOpt(default)

  val panelHeading = classNamesStyle("panel-heading")
  val panelBody = classNamesStyle("panel-body")
  val panelTitle = classNamesStyle("panel-title")

  val dropdown = classNamesStyle("dropdown")
  val dropdownMenu = classNamesStyle("dropdown-menu")

  object modal {
    val modal = classNamesStyle("modal")
    val fade = classNamesStyle("fade")
    val dialog = classNamesStyle("modal-dialog")
    val content = classNamesStyle("modal-content")
    val header = classNamesStyle("modal-header")
    val body = classNamesStyle("modal-body")
    val footer = classNamesStyle("modal-footer")
  }

  val _modal = modal

  object listGroup {
    val listGroup = classNamesStyle("list-group")
    val itemOpt = commonStyle(contextDomain, "list-group-item")
    val item = classNamesStyle("list-group-item")
    val itemHeading = classNamesStyle("list-group-item-heading")
    val itemText = classNamesStyle("list-group-item-text")
  }

  val _listGroup = listGroup

  val collapsed = classNamesStyle("collapsed")
  val collapseCollapsed = classNamesStyle("collapse")
  val collapseExpanded = classNamesStyle("collapse", "in")

  val container = classNamesStyle("container")
  val row = classNamesStyle("row")

  val pullLeft = classNamesStyle("pull-left")
  val pullRight = classNamesStyle("pull-right")

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
  val formControlStatic = classNamesStyle("form-control-static")
  val formHorizontal = classNamesStyle("form-horizontal")

  val caret = classNamesStyle("caret")
}
