package org.khvostovets.deck_game_server.message

trait OutputMessage {
  def forUser(targetUser: String): Boolean
  //def forSession(sessionId: String): Boolean
  def toString: String
}

case class WelcomeUser(user: String) extends OutputMessage {
  override def forUser(targetUser: String): Boolean = targetUser == user
  override def toString: String                     = s"Welcome to GameServer example"
}

case class SendToUser(user: String, text: String) extends OutputMessage {
  override def forUser(targetUser: String): Boolean = targetUser == user
  override def toString: String                     = text
}

case class SendToUsers(users: Set[String], text: String) extends OutputMessage {
  override def forUser(targetUser: String): Boolean = users.contains(targetUser)
  override def toString: String                     = text
}

case object Ping extends OutputMessage {
  override def forUser(targetUser: String) = true
  override def toString: String            = "ping"
}