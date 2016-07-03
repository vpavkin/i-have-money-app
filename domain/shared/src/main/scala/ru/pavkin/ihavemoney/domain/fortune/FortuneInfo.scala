package ru.pavkin.ihavemoney.domain.fortune

case class FortuneInfo(
    id: String,
    owner: String,
    editors: Set[String],
    initializationMode: Boolean)
