package ru.pavkin.ihavemoney.protocol

import java.util.UUID

case class CommandProcessed(commandId: UUID)
case class CommandProcessedWithResult[T](commandId: UUID, result: T)
