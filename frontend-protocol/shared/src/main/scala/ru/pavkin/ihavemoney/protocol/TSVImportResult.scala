package ru.pavkin.ihavemoney.protocol

import io.circe.Encoder
import io.circe.generic.auto._

case class TSVImportResult(success: Int, failure: Int)
object TSVImportResult {
  implicit val encoder = Encoder[TSVImportResult]
}
