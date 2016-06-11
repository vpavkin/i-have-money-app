import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{BaseUrl, Redirect, Resolution, Router, RouterConfigDsl, RouterCtl}
import japgolly.scalajs.react.vdom.all._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLStyleElement
import ru.pavkin.ihavemoney.frontend.{Route, api}
import ru.pavkin.ihavemoney.frontend.Route._
import ru.pavkin.ihavemoney.frontend.components._
import ru.pavkin.ihavemoney.frontend.redux.AppCircuit
import ru.pavkin.ihavemoney.frontend.styles.Global

import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport


object IHaveMoneyApp extends JSApp {

  val routerConfig = RouterConfigDsl[Route].buildConfig { dsl ⇒
    import dsl._

    (trimSlashes
      | staticRoute(root, Preloader) ~> renderR(ctl ⇒
      AppCircuit.connect(identity(_))(proxy => PreloaderComponent.component(PreloaderComponent.Props(ctl, proxy))))
      | staticRoute("#nofortune", NoFortunes) ~> renderR(ctl ⇒ NoFortuneComponent.component(ctl))
      | staticRoute("#transactions", AddTransactions) ~> render(AddTransactionsComponent.component())
      | staticRoute("#balance", BalanceView) ~> render(AppCircuit.connect(_.balances)(b ⇒ BalanceViewComponent.component(BalanceViewComponent.Props(b))))
      | staticRoute("#login", Login) ~> renderR(ctl ⇒ LoginComponent.component(ctl)))
      .notFound(redirectToPage(AddTransactions)(Redirect.Replace))
      .renderWith(layout)
      .verify(AddTransactions, BalanceView)
  }

  def layout(c: RouterCtl[Route], r: Resolution[Route]) = div(
    r.page match {
      case Login | Preloader ⇒ EmptyTag
      case _ ⇒ Nav.component(c)
    },
    div(className := "container", r.render())
  )

  @JSExport
  def main(): Unit = {
    dom.document.head appendChild Global.render[HTMLStyleElement]
    val router = Router(api.readFrontBaseUrl, routerConfig.logToConsole)
    router() render dom.document.getElementById("root")
  }
}
