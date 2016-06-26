package ru.pavkin.ihavemoney.writeback.services

import ru.pavkin.ihavemoney.services.EmailService

import scala.concurrent.{ExecutionContext, Future}

class DummyEmailService extends EmailService {
  def sendEmail(from: String, to: String, subject: String, content: String)(implicit ec: ExecutionContext): Future[Unit] =
    Future.successful(println(s"Sending $subject to $to"))
}
