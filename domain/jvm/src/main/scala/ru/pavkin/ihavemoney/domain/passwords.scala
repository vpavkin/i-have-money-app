package ru.pavkin.ihavemoney.domain

import com.github.t3hnar.bcrypt._

object passwords {

  private val ROUNDS_COUNT = 10

  def encrypt(password: String): String = password.bcrypt(ROUNDS_COUNT)

  def isCorrect(password: String, hash: String): Boolean = password.isBcrypted(hash)
}
