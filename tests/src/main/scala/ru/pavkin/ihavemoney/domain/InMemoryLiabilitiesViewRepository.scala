package ru.pavkin.ihavemoney.domain

import ru.pavkin.ihavemoney.domain.fortune._
import ru.pavkin.ihavemoney.readback.repo.{AssetsViewRepository, LiabilitiesViewRepository}

import scala.concurrent.{ExecutionContext, Future}

class InMemoryLiabilitiesViewRepository extends LiabilitiesViewRepository with InMemoryRepository[(LiabilityId, FortuneId), Liability]
