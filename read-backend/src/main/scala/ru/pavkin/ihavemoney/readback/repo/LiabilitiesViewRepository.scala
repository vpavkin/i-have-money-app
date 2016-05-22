package ru.pavkin.ihavemoney.readback.repo

import ru.pavkin.ihavemoney.domain.fortune.{FortuneId, Liability, LiabilityId}

trait LiabilitiesViewRepository extends Repository[(LiabilityId, FortuneId), Liability]
