package ru.pavkin.ihavemoney.frontend.gravatar

object GravatarAPI {

  def digest(s: String): String = scala.scalajs.js.Dynamic.global.md5(s).toString

  def hash(email: String) =
    digest(email)

  def img(email: String, size: Int = 80): String =
    s"https://www.gravatar.com/avatar/${hash(email)}?s=$size"

}
