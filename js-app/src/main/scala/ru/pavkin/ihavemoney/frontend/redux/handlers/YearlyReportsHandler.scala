package ru.pavkin.ihavemoney.frontend.redux.handlers

import diode.ActionResult.EffectOnly
import diode.{ActionHandler, Effect, ModelRW}
import japgolly.scalajs.react.vdom.all._
import ru.pavkin.ihavemoney.frontend.api
import ru.pavkin.ihavemoney.frontend.api.readFrontBaseUrl
import ru.pavkin.ihavemoney.frontend.components.{ErrorModal, SuccessModal}
import ru.pavkin.ihavemoney.frontend.redux.actions.{DownloadYearlyReport, ShowModal}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class YearlyReportsHandler[M](modelRW: ModelRW[M, Unit]) extends ActionHandler(modelRW) {

  override def handle: PartialFunction[Any, EffectOnly] = {
    case action: DownloadYearlyReport =>
      EffectOnly(Effect(
        api.generateYearlyReport(action.year)
          .map {
            case Left(e) => ShowModal(ErrorModal(e))
            case Right(reportUrl) => ShowModal(SuccessModal(div(
              h3("Your report is ready and available ", a(href := (readFrontBaseUrl.value + reportUrl), "here"), ".")
            )))
          }
          .recover {
            case e: Throwable => ShowModal(ErrorModal(e))
          }
      ))
  }
}


