package ru.pavkin.ihavemoney.frontend.components

import cats.data.Xor
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom.raw.HTMLInputElement
import ru.pavkin.ihavemoney.frontend.{Route, api}
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LogIn
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.ScalaCssReact._

object LoginComponent {

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
        case Xor.Left(error) ⇒ Callback.alert(error.getMessage)
        case Xor.Right(auth) ⇒ Callback {
          AppCircuit.dispatch(LogIn(auth))
        }.flatMap(_ ⇒ router.set(Route.AddTransactions))
      }
    )

    def register(email: String, password: String, displayName: String) = Callback.future(
      api.register(email, password, displayName).map {
        case Xor.Left(error) ⇒ Callback.alert(error.getMessage)
        case Xor.Right(_) ⇒ Callback.alert("Check your email for confirmation link")
      }
    )

    def render(router: Props, state: State) = {
      div(
        form(
          className := "form-horizontal",
          onSubmit ==> onFormSubmit,
          div(className := "form-group",
            label(htmlFor := "emailInput", className := "col-md-2 control-label", "Email"),
            div(className := "col-md-10",
              input(
                ref := emailInput,
                required := true,
                tpe := "text",
                className := "form-control",
                id := "emailInput",
                placeholder := "Email",
                value := state.email,
                onChange ==> onTextChange((s, v) ⇒ s.copy(email = v))
              )
            )),
          div(className := "form-group",
            label(htmlFor := "passwordInput", className := "col-md-2 control-label", "Password"),
            div(className := "col-md-10",
              input(
                ref := passwordInput,
                required := true,
                tpe := "password",
                className := "form-control",
                id := "passwordInput",
                placeholder := "Password",
                value := state.password,
                onChange ==> onTextChange((s, v) ⇒ s.copy(password = v))
              )
            )),
          div(className := "form-group",
            label(htmlFor := "displayNameInput", className := "col-md-2 control-label", "Name"),
            div(className := "col-md-10",
              input(
                ref := passwordInput,
                required := true,
                tpe := "text",
                className := "form-control",
                id := "displayNameInput",
                placeholder := "Name (for registration)",
                value := state.displayName,
                onChange ==> onTextChange((s, v) ⇒ s.copy(displayName = v))
              )
            )),
          div(className := "form-group col-md-12", alignedRight,
            button(tpe := "submit", buttonMarginRight,
              className := "btn btn-success", disabled := state.email.isEmpty || state.password.isEmpty,
              onClick --> login(router)(state.email, state.password), "Login"),
            button(tpe := "submit", className := "btn btn-success", disabled := state.email.isEmpty || state.password.isEmpty,
              onClick --> register(state.email, state.password, state.displayName), "Register")
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
