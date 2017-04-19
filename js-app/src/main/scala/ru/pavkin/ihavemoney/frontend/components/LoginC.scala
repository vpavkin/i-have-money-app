package ru.pavkin.ihavemoney.frontend.components


import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.raw.HTMLInputElement
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, FormGroup}
import ru.pavkin.ihavemoney.frontend.{Route, api}
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LoggedIn
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.ScalaCssReact._

object LoginC {

  type Props = RouterCtl[Route]

  case class State(email: String,
                   password: String,
                   displayName: String)

  class Backend($: BackendScope[Props, State]) {

    val emailInput = Ref[HTMLInputElement]("emailInput")
    val passwordInput = Ref[HTMLInputElement]("passwordInput")

    def onTextChange(change: (State, String) ⇒ State)(e: ReactEventI) = {
      val newValue = e.target.value
      $.modState(change(_, newValue))
    }

    def onFormSubmit(e: ReactEventI) = e.preventDefaultCB

    def login(router: RouterCtl[Route])(email: String, password: String) = Callback.future(
      api.login(email, password).map {
        case Left(error) ⇒ Callback.alert(error.getMessage)
        case Right(auth) ⇒ Callback {
          AppCircuit.dispatch(LoggedIn(auth))
        }.flatMap(_ ⇒ router.set(Route.Initializer))
      }
    )

    def register(email: String, password: String, displayName: String) = Callback.future(
      api.register(email, password, displayName).map {
        case Left(error) ⇒ Callback.alert(error.getMessage)
        case Right(_) ⇒ Callback.alert("Check your email for confirmation link")
      }
    )

    def render(router: Props, state: State) = {
      div(
        h1(centered, "Welcome to I Have Money!"),
        h3(centered, "Please, log in:"),
        form(
          topMargin(20),
          increasedFontSize, common.formHorizontal,
          onSubmit ==> onFormSubmit,
          FormGroup(
            HorizontalForm.Label("Email", "loginInput"),
            div(HorizontalForm.input, input.text(
              ref := emailInput,
              id := "emailInput",
              common.formControl,
              increasedFontSize,
              required := true,
              placeholder := "Email",
              value := state.email,
              onChange ==> onTextChange((s, v) ⇒ s.copy(email = v))
            )
            )),
          FormGroup(
            HorizontalForm.Label("Password", "passwordInput"),
            div(HorizontalForm.input, input.password(
              ref := passwordInput,
              id := "passwordInput",
              common.formControl,
              increasedFontSize,
              required := true,
              placeholder := "Password",
              value := state.password,
              onChange ==> onTextChange((s, v) ⇒ s.copy(password = v))
            )
            )),
          FormGroup(
            HorizontalForm.Label("Name", "displayNameInput"),
            div(HorizontalForm.input, input.text(
              id := "displayNameInput",
              common.formControl,
              increasedFontSize,
              required := true,
              placeholder := "Name (for registration)",
              value := state.displayName,
              onChange ==> onTextChange((s, v) ⇒ s.copy(displayName = v))
            )
            )),
          FormGroup(
            div(grid.columnOffsetAll(2), grid.columnAll(10),
              Button(
                login(router)(state.email, state.password),
                common.context.success,
                addStyles = Seq(common.buttonLarge, rightMargin),
                addAttributes = Seq(disabled := state.email.isEmpty || state.password.isEmpty))("Log In"),
              Button(
                register(state.email, state.password, state.displayName),
                common.context.warning,
                addStyles = Seq(common.buttonLarge, rightMargin),
                addAttributes = Seq(disabled := state.email.isEmpty || state.password.isEmpty || state.displayName.isEmpty))("Register")
            )
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("Login")
    .initialState(State("", "", ""))
    .renderBackend[Backend]
    .build
}
