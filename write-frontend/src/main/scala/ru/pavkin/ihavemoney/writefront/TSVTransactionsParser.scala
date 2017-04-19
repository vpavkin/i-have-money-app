package ru.pavkin.ihavemoney.writefront

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

import io.funcqrs.CommandId
import ru.pavkin.ihavemoney.domain.fortune.FortuneProtocol.Spend
import ru.pavkin.ihavemoney.domain.fortune.{Currency, ExpenseCategory, Worth}
import ru.pavkin.ihavemoney.domain.user.UserId
import ru.pavkin.utils.option._

import scala.util.{Failure, Try}

object TSVTransactionsParser {

  private def failure(s: String) = Failure(new Exception(s"Not a tsv row: $s"))

  private def singleWorth(amount: BigDecimal, currency: Currency): List[Worth] =
    if (amount > 0) List(Worth(amount, currency)) else Nil

  private def worth(usd: BigDecimal, eur: BigDecimal, rur: BigDecimal): List[Worth] =
    singleWorth(usd, Currency.USD) ++ singleWorth(eur, Currency.EUR) ++ singleWorth(rur, Currency.RUR)

  private val catsMap = Map(
    "Продукты" -> "Groceries",
    "Обеды" -> "Lunches",
    "Развлечения" -> "Entertainment",
    "Связь" -> "Communication",
    "Подарки" -> "Presents",
    "Путешествия" -> "Traveling",
    "Транспорт" -> "Transport",
    "Творчество" -> "Creativity",
    "Спорт" -> "Sport",
    "Здоровье" -> "Health",
    "Красота" -> "Beauty",
    "Одежда" -> "Clothes",
    "Квартира" -> "Rent & Utility",
    "Другое" -> "Other"
  )
  private def mapCategory(cat: String): ExpenseCategory =
    ExpenseCategory(catsMap.getOrElse(cat, cat))

  def parseLine(userId: UserId)(s: String): Try[List[Spend]] = s.split("\t").map(_.trim).toList match {
    case date :: category :: usd :: eur :: rur :: comment :: Nil => for {
      d <- Try(LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy")))
      u <- Try(BigDecimal(usd.replace(",", ".").replaceAll("\\s", "")))
      e <- Try(BigDecimal(eur.replace(",", ".").replaceAll("\\s", "")))
      r <- Try(BigDecimal(rur.replace(",", ".").replaceAll("\\s", "")))
    } yield worth(u, e, r).map(w => Spend(
      CommandId(UUID.randomUUID()),
      userId,
      w.amount,
      w.currency,
      mapCategory(category),
      Some(d),
      initializer = false,
      notEmpty(comment))
    )

    case _ => failure(s)
  }
}
