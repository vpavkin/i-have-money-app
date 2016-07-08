package ru.pavkin.ihavemoney.frontend.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.Route
import ru.pavkin.ihavemoney.frontend.Route._
import ru.pavkin.ihavemoney.frontend.bootstrap.attributes._
import ru.pavkin.ihavemoney.frontend.bootstrap.{Button, Dropdown}
import ru.pavkin.ihavemoney.frontend.gravatar.GravatarAPI
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.redux.actions.LoggedOut
import ru.pavkin.ihavemoney.frontend.styles.Global._

import scalacss.ScalaCssReact._

object NavigationBar {

  case class State(token: String)

  class Backend($: BackendScope[RouterCtl[Route], State]) {

    def logOut(router: RouterCtl[Route])(e: ReactEventI) = e.preventDefaultCB >>
        Callback(AppCircuit.dispatch(LoggedOut)) >>
        router.set(Route.Login)

    def render(ctl: RouterCtl[Route], s: State) = {
      def routeLink(name: String, target: Route) =
        li(a(href := ctl.urlFor(target).value, onClick ==> ctl.setEH(target), name))

      nav(common.navbarFixedTop, common.navbar,
        div(common.container,
          div(common.navbarHeader,
            Button.component(Button.Props(addAttributes = Seq(
              className := "navbar-toggle collapsed",
              dataToggle := "collapse",
              dataTarget := "#navbar",
              aria.expanded := "false",
              aria.controls := "navbar",
              span(className := "sr-only", "Toggle navigation"),
              span(className := "icon-bar"),
              span(className := "icon-bar"),
              span(className := "icon-bar")
            ))),
            a(common.navbarBrand, href := "#", "I Have Money")
          ),
          div(id := "navbar", common.navbarCollapse,
            aria.expanded := "false",
            ul(common.navbarSection,
              routeLink("Expenses", Expenses),
              routeLink("Income", Income),
              routeLink("Balance View", BalanceView),
              routeLink("TransactionLog", TransactionLogView),
              routeLink("Settings", FortuneSettingsView)
            ),
            ul(common.navbarSection, common.navbarRight,
              Dropdown.component.withKey("loggedInAs")(Dropdown.Props(
                table(tbody(tr(
                  td(img(className := "img-circle", paddingRight := 10, src := GravatarAPI.img(AppCircuit.auth.map(_.email).getOrElse(""), 42))),
                  td(span("Logged in as", br(), strong(AppCircuit.auth.map(_.displayName).getOrElse(""): String)))
                ))),
                li, showCaret = false, addStyles = Seq(navbarTwoLineItem, loggedInAsNavbarItem)),
                li(a("Log out", onClick ==> logOut(ctl)))
              )
            )
          )
        )
      )
    }
  }

  val component = ReactComponentB[RouterCtl[Route]]("Menu")
      .initialState(State(""))
      .renderBackend[Backend]
      .build
}
