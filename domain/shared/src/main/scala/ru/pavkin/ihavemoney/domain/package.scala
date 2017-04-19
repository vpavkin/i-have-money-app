package ru.pavkin.ihavemoney

package object domain extends PlatformDomain {
  def unexpected = throw new Exception("unexpected")
}
