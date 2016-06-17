package ru.pavkin.ihavemoney.protocol

import java.time.LocalDate

import ru.pavkin.ihavemoney.domain.fortune.Currency

case class Transaction(user: String,
                       amount: BigDecimal,
                       currency: Currency,
                       category: String,
                       initializer: Boolean,
                       date: LocalDate,
                       comment: Option[String])
