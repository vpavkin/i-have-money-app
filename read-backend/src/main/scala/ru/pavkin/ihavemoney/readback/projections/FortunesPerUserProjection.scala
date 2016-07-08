package ru.pavkin.ihavemoney.readback.projections

import io.funcqrs.{HandleEvent, Projection}
import ru.pavkin.ihavemoney.domain.fortune.FortuneId
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.user.UserId
import ru.pavkin.ihavemoney.readback.repo.Repository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FortunesPerUserProjection(repo: FortunesPerUserProjection.Repo) extends Projection {

  def update(id: UserId, updater: Set[FortuneId] ⇒ Set[FortuneId]) =
    repo.byId(id).map(_.getOrElse(Set.empty[FortuneId]))
      .flatMap(s ⇒ repo.replaceById(id, updater(s)))

  def handleEvent: HandleEvent = {

    case evt: FortuneEvent ⇒ evt match {
      case e: FortuneCreated ⇒
        update(e.owner, _ + e.aggregateId)
      case e: EditorAdded ⇒
        update(e.editor, _ + e.aggregateId)
      case _ ⇒ Future.successful(())
    }
  }
}

object FortunesPerUserProjection {
  type Repo = Repository[UserId, Set[FortuneId]]
}
