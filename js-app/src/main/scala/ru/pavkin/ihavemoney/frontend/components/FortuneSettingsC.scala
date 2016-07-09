package ru.pavkin.ihavemoney.frontend.components

import cats.data.Xor
import cats.syntax.traverse._
import cats.std.option._
import cats.std.list._
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExpenseCategory, FortuneInfo, Worth}
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup}
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scala.math.BigDecimal
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Try
import scalacss.ScalaCssReact._

object FortuneSettingsC {

  case class Props(fortunes: ModelProxy[Pot[List[FortuneInfo]]])

  case class State(email: String, weeklyLimits: String, monthlyLimits: String) {
    val weekly = parseLimits(weeklyLimits)
    val monthly = parseLimits(monthlyLimits)
    def limitsValid = weekly.isDefined && monthly.isDefined
  }

  class Backend($: BackendScope[Props, State]) {

    def onFormSubmit(e: ReactEventI): Callback = e.preventDefaultCB

    def onTextChange(change: (State, String) ⇒ State)(e: ReactEventI) = {
      val newValue = e.target.value
      $.modState(change(_, newValue))
    }

    def updateLimits(s: State) = if (!s.limitsValid) Callback.alert("Invalid limits config")
    else Callback.future(
      api.updateLimits(s.weekly.get, s.monthly.get).map {
        case Xor.Left(error) ⇒ Callback.alert(error.getMessage)
        case Xor.Right(_) ⇒ Callback.alert("Done, please refresh the page!")
      }
    )

    def addEditor(email: String) = Callback.future(
      api.addEditor(email).map {
        case Xor.Left(error) ⇒ Callback.alert(error.getMessage)
        case Xor.Right(_) ⇒ Callback.alert("Done, please refresh the page!")
      }
    )

    def finishInitialization = Callback.future(
      api.finishInitialization.map {
        case Xor.Left(error) ⇒ Callback.alert(error.getMessage)
        case Xor.Right(_) ⇒ Callback.alert("Done, please refresh the page!")
      }
    )

    def renderInitializationOffButton(f: FortuneInfo) =
      if (f.initializationMode) div(
        hr(),
        h2("Fortune is in initialization mode."),
        Button(
          finishInitialization,
          common.context.warning,
          addStyles = Seq(common.buttonLarge))("Finish Initialization")
      )
      else
        EmptyTag

    def renderLimitsEditor(s: State) = div(
      hr(),
      h2("Limits"),
      div(common.container,
        div(grid.columnAll(6),
          form(FormGroup(
            label("Weekly limits:"),
            textarea(className := "form-control", rows := 10, value := s.weeklyLimits, onChange ==> onTextChange((s, v) ⇒ s.copy(weeklyLimits = v)))
          ))
        ),
        div(grid.columnAll(6),
          form(FormGroup(
            label("Monthly limits:"),
            textarea(className := "form-control", rows := 10, value := s.monthlyLimits, onChange ==> onTextChange((s, v) ⇒ s.copy(monthlyLimits = v)))
          ))
        )
      ),
      div(Button(
        updateLimits(s),
        common.context.warning,
        addStyles = Seq(common.buttonLarge),
        addAttributes = Seq(disabled := !s.limitsValid))("Update Limits"))
    )

    def renderAddEditorForm(s: State, f: FortuneInfo) = div(common.container,
      form(
        topMargin(20),
        increasedFontSize, common.formHorizontal,
        onSubmit ==> onFormSubmit,
        FormGroup(
          HorizontalForm.Label("Email", "emailInput"),
          div(HorizontalForm.input, input.email(
            id := "emailInput",
            common.formControl,
            increasedFontSize,
            required := true,
            placeholder := "Editor email",
            value := s.email,
            onChange ==> onTextChange((s, v) ⇒ s.copy(email = v))
          )
          )),
        FormGroup(
          div(grid.columnOffsetAll(2), grid.columnAll(10),
            Button(
              addEditor(s.email),
              common.context.success,
              addStyles = Seq(common.buttonLarge),
              addAttributes = Seq(disabled := s.email.isEmpty))("Add editor")
          )
        )
      )
    )

    def render(pr: Props, s: State) = div(
      pr.fortunes().renderEmpty(PreloaderC()),
      pr.fortunes().renderPending(_ => PreloaderC()),
      pr.fortunes().renderReady { fortunes ⇒
        val f = fortunes.head
        div(
          h1(s"Owner: ${f.owner}"),
          if (f.editors.nonEmpty) div(
            h2("Editors:"),
            ul(
              f.editors.map(li(_))
            ))
          else EmptyTag,
          hr(),
          h2("Add editor:"),
          renderAddEditorForm(s, f),
          renderLimitsEditor(s),
          renderInitializationOffButton(f)
        )
      }
    )
  }

  def stringifyLimits(l: Map[ExpenseCategory, Worth]) = l.map {
    case (k, v) ⇒ s"${k.name} -> ${v.toString}"
  }.toList.sorted.mkString("\n")

  def parseWorth(s: String): Option[Worth] = s.split(" ").toList match {
    case amount :: currency :: Nil ⇒ for {
      a ← Try(BigDecimal(amount)).toOption
      c ← Currency.fromCode(currency)
    } yield Worth(a, c)
    case _ ⇒ None
  }

  def parseLimits(s: String): Option[Map[ExpenseCategory, Worth]] =
    s.trim.split("\n").filterNot(_.isEmpty).map(
      _.trim.split("->").toList.map(_.trim) match {
        case key :: value :: Nil ⇒
          parseWorth(value).map(w ⇒ ExpenseCategory(key) → w)
        case _ ⇒ None
      }
    ).toList.sequence.map(_.toMap)

  val component = ReactComponentB[Props]("Fortune settings")
      .initialState(State("",
        stringifyLimits(AppCircuit.fortune.weeklyLimits),
        stringifyLimits(AppCircuit.fortune.monthlyLimits)
      ))
      .renderBackend[Backend]
      .build

  def apply(fortunes: ModelProxy[Pot[List[FortuneInfo]]]) = component(Props(fortunes))
}
