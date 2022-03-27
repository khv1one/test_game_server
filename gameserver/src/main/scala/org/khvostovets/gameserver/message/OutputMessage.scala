package org.khvostovets.gameserver.message

import org.khvostovets.user.User

trait OutputMessage {
  def forUser(targetUser: String): Boolean
  def toString: String
}

case class WelcomeUser(user: String) extends OutputMessage {
  override def forUser(targetUser: String): Boolean = targetUser == user
  override def toString: String = s"Welcome to GameServer example"
}

case class SendToEndpoint(endpoint: String, text: String) extends OutputMessage {
  override def forUser(targetUser: String): Boolean = targetUser == endpoint
  override def toString: String = text
}

case class SendToUser(user: User, text: String) extends OutputMessage {
  override def forUser(targetUser: String): Boolean = targetUser == user.name
  override def toString: String = text
}

case class SendToUsers(users: Set[User], text: String) extends OutputMessage {
  override def forUser(targetUser: String): Boolean = users.map(_.name).contains(targetUser)
  override def toString: String = text
}

case object Ping extends OutputMessage {
  override def forUser(targetUser: String) = true
  override def toString: String = "ping"
}