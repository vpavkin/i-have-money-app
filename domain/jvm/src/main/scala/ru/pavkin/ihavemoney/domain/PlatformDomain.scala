package ru.pavkin.ihavemoney.domain

import java.util.UUID

import io.funcqrs.CommandId

trait PlatformDomain {
  def cmdId = CommandId(UUID.randomUUID())
}
