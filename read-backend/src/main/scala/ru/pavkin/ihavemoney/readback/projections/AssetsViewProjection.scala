package ru.pavkin.ihavemoney.readback.projections

import io.funcqrs.{HandleEvent, Projection}
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.fortune.{Currency, FortuneId}
import ru.pavkin.ihavemoney.readback.repo.{AssetsViewRepository, LiabilitiesViewRepository, MoneyViewRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssetsViewProjection(assetRepo: AssetsViewRepository) extends Projection {

  def handleEvent: HandleEvent = {

    case evt: FortuneEvent ⇒ evt match {
      case AssetAcquired(user, assetId, asset, initializer, metadata, comment) =>
        assetRepo.insert(assetId → evt.aggregateId, asset)
      case AssetSold(user, assetId, metadata, comment) =>
        assetRepo.remove(assetId → evt.aggregateId)
      case AssetWorthChanged(user, assetId, newAmount, metadata, comment) =>
        assetRepo.updateById(assetId → evt.aggregateId, _.reevaluate(newAmount))
      case _ ⇒ Future.successful(())
    }
  }
}
