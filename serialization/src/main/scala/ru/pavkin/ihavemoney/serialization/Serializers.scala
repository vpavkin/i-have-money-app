package ru.pavkin.ihavemoney.serialization

import ru.pavkin.ihavemoney.domain.CommandEnvelope
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol._
import ru.pavkin.ihavemoney.domain.user.UserProtocol.{ConfirmEmail, CreateUser, LoginUser, ResendConfirmationEmail}
import ru.pavkin.ihavemoney.proto.commands._
import ru.pavkin.ihavemoney.serialization.implicits._

class CommandEnvelopeSerializer extends ProtobufSerializer[CommandEnvelope, PBCommandEnvelope](100)
class ReceiveIncomeSerializer extends ProtobufSerializer[ReceiveIncome, PBReceiveIncome](101)
class SpendSerializer extends ProtobufSerializer[Spend, PBSpend](102)
class CreateFortuneSerializer extends ProtobufSerializer[CreateFortune, PBCreateFortune](103)
class AddEditorSerializer extends ProtobufSerializer[AddEditor, PBAddEditor](104)
class CreateUserSerializer extends ProtobufSerializer[CreateUser, PBCreateUser](105)
class LoginUserSerializer extends ProtobufSerializer[LoginUser, PBLogIn](106)
class ConfirmEmailSerializer extends ProtobufSerializer[ConfirmEmail, PBConfirmEmail](107)
class ResendConfirmationEmailSerializer extends ProtobufSerializer[ResendConfirmationEmail, PBResendConfirmationEmail](108)
class FinishInitializationSerializer extends ProtobufSerializer[FinishInitialization, PBFinishInitialization](109)
