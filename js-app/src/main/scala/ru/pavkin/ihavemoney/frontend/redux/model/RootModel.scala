package ru.pavkin.ihavemoney.frontend.redux.model

import diode.data.Pot
import ru.pavkin.ihavemoney.protocol.Auth

case class RootModel(auth: Option[Auth],
                     fortuneId: Pot[String] = Pot.empty)


