package ru.pavkin.ihavemoney.readback.projections

import io.funcqrs.{HandleEvent, Projection}
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.fortune.{FortuneId, FortuneInfo}
import ru.pavkin.ihavemoney.readback.repo.Repository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FortuneInfoProjection(repo: FortuneInfoProjection.Repo) extends Projection {

  def update(id: FortuneId, updater: FortuneInfo ⇒ FortuneInfo) =
    repo.byId(id).map(_.get)
        .flatMap(s ⇒ repo.replaceById(id, updater(s)))

  def handleEvent: HandleEvent = {

    case evt: FortuneEvent ⇒ evt match {
      case e: FortuneCreated ⇒
        repo.insert(e.aggregateId, FortuneInfo(e.aggregateId.value, e.owner.value, Set.empty, Map.empty, Map.empty, initializationMode = true))
      case e: EditorAdded ⇒
        update(e.aggregateId, f ⇒ f.copy(editors = f.editors + e.editor.value))
      case e: FortuneInitializationFinished ⇒
        update(e.aggregateId, _.copy(initializationMode = false))
      case e: LimitsUpdated ⇒
        update(e.aggregateId, _.copy(weeklyLimits = e.weekly, monthlyLimits = e.monthly))
      case _ ⇒ Future.successful(())
    }
  }
}

object FortuneInfoProjection {
  type Repo = Repository[FortuneId, FortuneInfo]
}
