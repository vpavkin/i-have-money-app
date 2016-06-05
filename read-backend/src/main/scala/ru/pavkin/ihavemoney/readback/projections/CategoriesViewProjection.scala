package ru.pavkin.ihavemoney.readback.projections

import io.funcqrs.{HandleEvent, Projection}
import ru.pavkin.ihavemoney.domain.fortune.FortuneId
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.readback.repo.Repository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CategoriesViewProjection(repo: CategoriesViewProjection.Repo) extends Projection {

  def addCategory(id: FortuneId, category: IncomeCategory) =
    repo.updateById(id,
      tpl ⇒ (tpl._1 + category) → tpl._2
    )

  def addCategory(id: FortuneId, category: ExpenseCategory) =
    repo.updateById(id,
      tpl ⇒ tpl._1 → (tpl._2 + category)
    )

  def handleEvent: HandleEvent = {

    case evt: FortuneEvent ⇒ evt match {
      case e: FortuneIncreased ⇒
        addCategory(e.metadata.aggregateId, e.category)
      case e: FortuneSpent ⇒
        addCategory(e.metadata.aggregateId, e.category)
      case _ ⇒ Future.successful(())
    }
  }
}

object CategoriesViewProjection {
  type Repo = Repository[FortuneId, (Set[IncomeCategory], Set[ExpenseCategory])]
}
