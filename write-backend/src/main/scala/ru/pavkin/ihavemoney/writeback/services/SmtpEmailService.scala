package ru.pavkin.ihavemoney.writeback.services

import com.typesafe.config.Config
import org.apache.commons.mail.{DefaultAuthenticator, HtmlEmail}
import ru.pavkin.ihavemoney.services.EmailService

import scala.concurrent.{ExecutionContext, Future}

class SmtpEmailService(config: SmtpConfig) extends EmailService {

  def sendEmail(from: String, to: String, subject: String, content: String)(implicit ec: ExecutionContext): Future[Unit] = Future {
    val email = new HtmlEmail()
    email.setStartTLSEnabled(config.tls)
    email.setSSLOnConnect(config.ssl)
    email.setSmtpPort(config.port)
    email.setHostName(config.host)
    email.setAuthenticator(new DefaultAuthenticator(
      config.user,
      config.password
    ))
    email.setHtmlMsg(content)
      .addTo(to)
      .setFrom(from)
      .setSubject(subject)
      .send()
  }
}

case class SmtpConfig(tls: Boolean,
                      ssl: Boolean,
                      port: Int,
                      host: String,
                      user: String,
                      password: String)

object SmtpConfig {
  def load(config: Config): SmtpConfig =
    SmtpConfig(
      config.getBoolean("smtp.tls"),
      config.getBoolean("smtp.ssl"),
      config.getInt("smtp.port"),
      config.getString("smtp.host"),
      config.getString("smtp.user"),
      config.getString("smtp.password")
    )
}
