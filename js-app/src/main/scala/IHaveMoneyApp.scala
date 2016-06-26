import diode.data._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{Redirect, Resolution, Router, RouterConfigDsl, RouterCtl}
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom
import ru.pavkin.ihavemoney.frontend.components._
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
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

    def renderAddTransactions = render(AddTransactionsC.component())
    def renderInitializer = renderR(InitializerC(_))

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
      | staticRoute("#transactions", Route.AddTransactions) ~> renderAddTransactions
      | staticRoute("#balance", Route.BalanceView) ~> render(AppCircuit.connect(_.balances)(b ⇒ BalanceViewC.component(BalanceViewC.Props(b))))
      | staticRoute("#log", Route.TransactionLogView) ~> render(AppCircuit.connect(_.log)(b ⇒ TransactionLogC.component(TransactionLogC.Props(b))))
      | staticRoute("#login", Route.Login) ~> renderR(ctl ⇒ LoginC.component(ctl)))
      .notFound(redirectToPage(Route.AddTransactions)(Redirect.Replace))
      .renderWith(layout)
      .onPostRender((prev, next) => Callback {
        println(s"Page changing from $prev to $next.")
        storeRedirectToRoute(prev, next)
      })
      .verify(Route.AddTransactions, Route.BalanceView)
  }

  def layout(c: RouterCtl[Route], r: Resolution[Route]) = div(
    r.page match {
      case Route.Login | Route.Initializer ⇒ r.render()
      case _ ⇒ div(
        NavigationBar.component(c),
        div(common.container, AppCircuit.connect(_.fortunes)(_ () match {
          case Ready(x) =>
            r.render()
          case Failed(exception) =>
            FatalErrorC.component(exception.getMessage)
          case _ => PreloaderC()
        }))
      )
    },
    AppCircuit.connect(_.modal)(px ⇒ div(px() match {
      case Some(element) ⇒ element
      case None ⇒ EmptyTag
    })),
    AppCircuit.connect(_.activeRequest)(px ⇒ div(px() match {
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
