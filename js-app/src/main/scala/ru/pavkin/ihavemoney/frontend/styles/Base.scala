package ru.pavkin.ihavemoney.frontend.styles

import japgolly.univeq.UnivEq

import scala.language.postfixOps
import scalacss.Defaults._

class Base(implicit r: StyleSheet.Register) extends StyleSheet.Inline()(r) {

  import dsl._

  val colorPrimary = c"#375a7f"
  val colorSuccess = c"#00bc8c"
  val colorInfo = c"#3498DB"
  val colorWarning = c"#F39C12"
  val colorDanger = c"#E74C3C"
  val colorBackground = c"#222"

  sealed trait ClassName {self =>
    def name: String = self.toString
  }

  sealed trait direction extends ClassName
  object direction {
    case object left extends direction
    case object right extends direction
    case object top extends direction
    case object bottom extends direction
  }

  // this can't start from uppercase because it breaks something in ScalaCSS internals
  sealed trait context extends ClassName
  object context {
    case object default extends context
    case object primary extends context
    case object success extends context
    case object info extends context
    case object warning extends context
    case object danger extends context
  }

  sealed abstract class Device(override val name: String) extends ClassName
  object Device {
    case object extraSmall extends Device("xs")
    case object small extends Device("sm")
    case object medium extends Device("md")
    case object large extends Device("lg")
  }

  // this can't start from uppercase because it breaks something in ScalaCSS internals
  sealed trait textStyle extends ClassName
  object textStyle {
    case object muted extends textStyle
    case object primary extends textStyle
    case object success extends textStyle
    case object info extends textStyle
    case object warning extends textStyle
    case object danger extends textStyle
  }

  import context._
  import Device._

  val commonDomain: Domain[context] = Domain.ofValues(default, primary, success, info, warning, danger)
  val contextDomain: Domain[context] = Domain.ofValues(success, info, warning, danger)
  val textDomain: Domain[textStyle] = Domain.ofValues(textStyle.muted, textStyle.primary, textStyle.success,
    textStyle.info, textStyle.warning, textStyle.danger)
  val deviceDomain: Domain[Device] = Domain.ofValues(extraSmall, small, Device.medium, large)
  val directionDomain: Domain[direction] = Domain.ofValues(direction.left, direction.right,
    direction.top, direction.bottom)

  implicit val contextUEq: UnivEq[context] = UnivEq.derive
  implicit val deviceUEq: UnivEq[Device] = UnivEq.derive
  implicit val textUEq: UnivEq[textStyle] = UnivEq.derive
  implicit val directionUEq: UnivEq[direction] = UnivEq.derive

  def domainStyle[A <: ClassName : UnivEq](domain: Domain[A]): (A) => StyleA = styleF(domain)(opt =>
    styleS(addClassNames(opt.name))
  )

  def commonStyle[A <: ClassName : UnivEq](domain: Domain[A], base: String): (A) => StyleA = styleF(domain)(opt =>
    styleS(addClassNames(base, s"$base-${opt.name}"))
  )

  def classNamesStyle(classNames: String*): StyleA = style(addClassNames(classNames: _*))

  val popoverDirection: direction => StyleA = domainStyle(directionDomain)
  val popover: StyleA = classNamesStyle("popover fade in")
  val popoverArrow: StyleA = classNamesStyle("arrow")
  val popoverTitle: StyleA = classNamesStyle("popover-title")
  val popoverBody: StyleA = classNamesStyle("popover-content")

  val disabled: StyleA = classNamesStyle("disabled")
  val active: StyleA = classNamesStyle("active")

  val hasErrorOpt: Boolean => StyleA = styleF(Domain.boolean)(b => if (b) addClassName("has-error") else StyleS.empty)

  val text: textStyle => StyleA = commonStyle(textDomain, "text")

  val buttonOpt: context => StyleA = commonStyle(commonDomain, "btn")
  val button: StyleA = buttonOpt(default)
  val buttonXS: StyleA = classNamesStyle("btn-xs")
  val buttonSM: StyleA = classNamesStyle("btn-sm")
  val buttonLarge: StyleA = classNamesStyle("btn-lg")
  val close: StyleA = classNamesStyle("close")

  val buttonToolbar: StyleA = classNamesStyle("btn-toolbar")
  val buttonGroup: StyleA = classNamesStyle("btn-group")

  val panelOpt: context => StyleA = commonStyle(commonDomain, "panel")
  val panel: StyleA = panelOpt(default)

  val labelOpt: context => StyleA = commonStyle(commonDomain, "label")
  val label: StyleA = labelOpt(default)

  val alertOpt: context => StyleA = commonStyle(commonDomain, "alert")
  val alert: StyleA = alertOpt(default)

  val panelHeading: StyleA = classNamesStyle("panel-heading")
  val panelBody: StyleA = classNamesStyle("panel-body")
  val panelTitle: StyleA = classNamesStyle("panel-title")

  val dropdown: StyleA = classNamesStyle("dropdown")
  val dropdownMenu: StyleA = classNamesStyle("dropdown-menu")

  val progress: StyleA = classNamesStyle("progress")
  val progressBarOpt: context => StyleA = commonStyle(commonDomain, "progress-bar")
  val progressBar: StyleA = progressBarOpt(default)

  object modal {
    val modal: StyleA = classNamesStyle("modal")
    val fade: StyleA = classNamesStyle("fade")
    val dialog: StyleA = classNamesStyle("modal-dialog")
    val content: StyleA = classNamesStyle("modal-content")
    val header: StyleA = classNamesStyle("modal-header")
    val body: StyleA = classNamesStyle("modal-body")
    val footer: StyleA = classNamesStyle("modal-footer")
  }

  val _modal: modal.type = modal

  object listGroup {
    val listGroup: StyleA = classNamesStyle("list-group")
    val itemOpt: context => StyleA = commonStyle(contextDomain, "list-group-item")
    val item: StyleA = classNamesStyle("list-group-item")
    val itemHeader: StyleA = classNamesStyle("list-group-item header")
    val itemHeading: StyleA = classNamesStyle("list-group-item-heading")
    val itemText: StyleA = classNamesStyle("list-group-item-text")
  }

  val _listGroup: listGroup.type = listGroup

  val collapsed: StyleA = classNamesStyle("collapsed")
  val collapseCollapsed: StyleA = classNamesStyle("collapse")
  val collapseExpanded: StyleA = collapseCollapsed + classNamesStyle("in")

  val container: StyleA = classNamesStyle("container")
  val row: StyleA = classNamesStyle("row")

  val pullLeft: StyleA = classNamesStyle("pull-left")
  val pullRight: StyleA = classNamesStyle("pull-right")

  val table: StyleA = classNamesStyle("table")
  val tableBordered: StyleA = classNamesStyle("table-bordered")
  val tableStriped: StyleA = classNamesStyle("table-striped")
  val tableCondensed: StyleA = classNamesStyle("table-condensed")

  val navbarOpt: context => StyleA = commonStyle(commonDomain, "navbar")
  val navbar: StyleA = navbarOpt(default)
  val navbarFixedTop: StyleA = classNamesStyle("navbar navbar-fixed-top")

  val navbarSection: StyleA = classNamesStyle("nav", "navbar-nav")
  val navbarHeader: StyleA = classNamesStyle("navbar-header")
  val navbarRight: StyleA = classNamesStyle("navbar-right")
  val navbarCollapse: StyleA = classNamesStyle("navbar-collapse", "collapse")
  val navbarBrand: StyleA = classNamesStyle("navbar-brand")

  val formGroup: StyleA = classNamesStyle("form-group")
  val formControl: StyleA = classNamesStyle("form-control")
  val formControlStatic: StyleA = classNamesStyle("form-control-static")
  val formHorizontal: StyleA = classNamesStyle("form-horizontal")
  val inputGroupAddon: StyleA = classNamesStyle("input-group-addon")
  val inputGroup: StyleA = classNamesStyle("input-group")

  val datePickerInputGroup: StyleA = classNamesStyle("date")

  val caret: StyleA = classNamesStyle("caret")

  val screenReaderOnly: StyleA = classNamesStyle("sr-only")

  val regularFontSize: StyleA = style(fontSize(100.%%))

  val listInline: StyleA = classNamesStyle("list-inline")

  val clearBoth: StyleA = style(clear.both)

  val inlineBlock: StyleA = style(display.inlineBlock)

  val fullWidth: StyleA = style(width(100.%%))

}
