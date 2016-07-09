package ru.pavkin.ihavemoney.domain.fortune

case class FortuneInfo(
    id: String,
    owner: String,
    editors: Set[String],
    weeklyLimits: Map[ExpenseCategory, Worth],
    monthlyLimits: Map[ExpenseCategory, Worth],
    initializationMode: Boolean)
