package ru.pavkin.ihavemoney.services

import scala.concurrent.{ExecutionContext, Future}

trait EmailService {
  def sendEmail(from: String, to: String, subject: String, content: String)(implicit ec: ExecutionContext): Future[Unit]
}
