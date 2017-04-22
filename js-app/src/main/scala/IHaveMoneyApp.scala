import diode.data._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{Redirect, Resolution, Router, RouterConfigDsl, RouterCtl}
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom
import ru.pavkin.ihavemoney.frontend.components._
import ru.pavkin.ihavemoney.frontend.redux.{AppCircuit, connectors}
import ru.pavkin.ihavemoney.frontend.redux.actions.{LoggedIn, SetInitializerRedirect}
import ru.pavkin.ihavemoney.frontend.styles.Global
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.ihavemoney.frontend.{Route, api}

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object IHaveMoneyApp extends JSApp {

  val routerConfig = RouterConfigDsl[Route].buildConfig { dsl ⇒
    import dsl._

    def renderExchange = render(connectors.log(l ⇒ ExchangeC(l)))
    def renderStats = render(connectors.log(b ⇒
      connectors.categories(c =>
        StatsViewC.component(StatsViewC.Props(AppCircuit.fortune, c, b))
      )
    ))
    def renderLog = render(connectors.log(b ⇒
      connectors.categories(c =>
        TransactionLogC.component(TransactionLogC.Props(b, c))
      )
    ))
    def renderExpenses = render(connectors.categories(c ⇒ ExpensesC(c)))
    def renderIncome = render(connectors.categories(c ⇒ IncomeC(c)))
    def renderInitializer = renderR(InitializerC(_))
    def renderBalance = render(connectors.balances(b ⇒
      connectors.assets(a ⇒
        connectors.liabilities(l ⇒
          BalanceViewC.component(BalanceViewC.Props(b, a, l)))
      )))
    def renderFortuneSettings = render(connectors.fortunes(f ⇒
      FortuneSettingsC(f)
    ))

    def isValidRedirect(r: Route) = r != Route.Login && r != Route.Initializer
    def storeRedirectToRoute(prev: Option[Route], next: Route) = (prev, next) match {
      case (Some(r), Route.Initializer) if isValidRedirect(r) ⇒
        AppCircuit.dispatch(SetInitializerRedirect(r))
      case (Some(r), Route.Login) if isValidRedirect(r) ⇒
        AppCircuit.dispatch(SetInitializerRedirect(r))
      case _ ⇒ ()
    }

    (trimSlashes
      | staticRoute(root, Route.Initializer) ~> renderInitializer
      | staticRoute("#nofortune", Route.NoFortunes) ~> renderR(ctl ⇒ NoFortuneC.component(ctl))
      | staticRoute("#expenses", Route.Expenses) ~> renderExpenses
      | staticRoute("#income", Route.Income) ~> renderIncome
      | staticRoute("#exchange", Route.Exchange) ~> renderExchange
      | staticRoute("#balance", Route.BalanceView) ~> renderBalance
      | staticRoute("#stats", Route.StatsView) ~> renderStats
      | staticRoute("#settings", Route.FortuneSettingsView) ~> renderFortuneSettings
      | staticRoute("#log", Route.TransactionLogView) ~> renderLog
      | staticRoute("#login", Route.Login) ~> renderR(ctl ⇒ LoginC.component(ctl)))
      .notFound(redirectToPage(Route.Expenses)(Redirect.Replace))
      .renderWith(layout)
      .onPostRender((prev, next) => Callback {
        println(s"Page changing from $prev to $next.")
        storeRedirectToRoute(prev, next)
      })
      .verify(Route.Expenses, Route.BalanceView)
  }

  def layout(c: RouterCtl[Route], r: Resolution[Route]) = div(
    r.page match {
      case Route.Login | Route.Initializer ⇒ r.render()
      case _ ⇒ div(
        NavigationBar.component(c),
        div(common.container, connectors.fortunes(_ () match {
          case Ready(x) =>
            r.render()
          case Failed(exception) =>
            FatalErrorC.component(exception.getMessage)
          case _ => PreloaderC()
        })),
        footer(
          div(commonFooter, cls := "version-footer", ru.pavkin.ihavemoney.BuildInfo.version)
        )
      )
    },
    connectors.modal(px ⇒ div(px() match {
      case Some(element) ⇒ element
      case None ⇒ EmptyTag
    })),
    connectors.activeRequest(px ⇒ div(px() match {
      case Pending(_) | PendingStale(_, _) => PreloaderC()
      case _ ⇒ EmptyTag
    }))
  )

  def renderRouter: RouterCtl[Route] = {
    val (router, ctl) = Router.componentAndCtl(api.readFrontBaseUrl, routerConfig.logToConsole)
    router().render(dom.document.getElementById("root"))
    ctl
  }

  @JSExport
  def main(): Unit = {
    Global.addToDocument
    val ctl = renderRouter

    AppCircuit.tryGetAuthFromLocalStorage match {
      case Some(a) ⇒
        AppCircuit.dispatch(LoggedIn(a))
        ctl.set(Route.Initializer).delayMs(200).void.runNow()
      case None ⇒
        ctl.set(Route.Login).delayMs(200).void.runNow()
    }
  }
}
