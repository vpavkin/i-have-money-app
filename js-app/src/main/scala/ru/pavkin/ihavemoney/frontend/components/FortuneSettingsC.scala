package ru.pavkin.ihavemoney.frontend.components

import cats.data.Xor
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.domain.fortune.FortuneInfo
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup}
import ru.pavkin.ihavemoney.frontend.styles.Global._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scalacss.ScalaCssReact._

object FortuneSettingsC {

  case class Props(fortunes: ModelProxy[Pot[List[FortuneInfo]]])

  case class State(email: String)

  class Backend($: BackendScope[Props, State]) {

    def onFormSubmit(e: ReactEventI): Callback = e.preventDefaultCB

    def onTextChange(change: (State, String) ⇒ State)(e: ReactEventI) = {
      val newValue = e.target.value
      $.modState(change(_, newValue))
    }

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
          renderInitializationOffButton(f)
        )
      }
    )
  }

  val component = ReactComponentB[Props]("Fortune settings")
      .initialState(State(""))
      .renderBackend[Backend]
      .build

  def apply(fortunes: ModelProxy[Pot[List[FortuneInfo]]]) = component(Props(fortunes))
}
