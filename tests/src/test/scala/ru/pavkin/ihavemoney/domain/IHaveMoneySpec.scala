package ru.pavkin.ihavemoney.domain

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}
import ru.pavkin.ihavemoney.domain.fortune.{Currency, Fortune, FortuneId, Worth}
import ru.pavkin.ihavemoney.domain.user.UserId

class IHaveMoneySpec extends FunSuite with Matchers with GeneratorDrivenPropertyChecks {

  lazy val currencies = Currency.values.toList

  val generateCurrency: Gen[Currency] = Gen.choose(0, currencies.size - 1).map(currencies(_))
  implicit val arbCurrency: Arbitrary[Currency] = Arbitrary(generateCurrency)

  val generateWorth: Gen[Worth] = for {
    c ← generateCurrency
    a ← Gen.posNum[Double].map(BigDecimal(_))
  } yield Worth(a, c)

  implicit val arbWorth: Arbitrary[Worth] = Arbitrary(generateWorth)

  val generateFortune: Gen[Fortune] = for {
    id ← Gen.uuid.map(_.toString).map(FortuneId(_))
    ownerId ← Gen.uuid.map(_.toString).map(UserId(_))
    balances ← Gen.mapOf(generateCurrency.flatMap(c ⇒ Gen.posNum[Double].map(BigDecimal(_)).map(b ⇒ c → b)))
  } yield Fortune(id, balances, Map.empty, Map.empty, ownerId, List.empty, Map.empty, Map.empty, Map.empty)

  implicit val arbFortune: Arbitrary[Fortune] = Arbitrary(generateFortune)
}
