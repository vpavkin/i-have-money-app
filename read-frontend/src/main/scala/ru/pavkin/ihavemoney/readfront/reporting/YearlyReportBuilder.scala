package ru.pavkin.ihavemoney.readfront.reporting

import java.util.UUID

import com.norbitltd.spoiwo.model.{Row, Sheet}
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions._

class YearlyReportBuilder(reportsDir: String) {

  def build: String = {
    val helloWorldSheet = Sheet(name = "Hello Sheet",
      row = Row().withCellValues("Hello World!")
    )
    val filePath = s"$reportsDir${UUID.randomUUID()}.xlsx"
    helloWorldSheet.saveAsXlsx(filePath)
    filePath
  }
}
