package org.khvostovets.gameserver.message

import org.khvostovets.user.User

trait InputMessage {
  val user: User
}

trait LobbyMessage extends InputMessage {
  val game: String
}

trait SessionMessage extends LobbyMessage {
  val session: String
}

case class Help(user: User)                                                             extends InputMessage
case class ListGames(user: User)                                                        extends InputMessage
case class InvalidInput(user: User, text: String)                                       extends InputMessage
case class Disconnect(user: User)                                                       extends InputMessage
case class EnterToLobby(user: User, game: String)                                       extends LobbyMessage
case class UsersInSession(user: User, game: String, session: String)                    extends SessionMessage
case class GameActionMessage(user: User, game: String, session: String, action: String) extends SessionMessage

object MessageParser {

  def parse(user: User, text: String): InputMessage =
    splitFirstTwoWords(text) match {
      case ("/help", _, _)           => Help(user)
      case ("/start", game, _)       => EnterToLobby(user, game)
      case ("/games", _, _)          => ListGames(user)
      case ("/users", game, session) => UsersInSession(user, game, session)
      case (s"/$cmd", game, session) => GameActionMessage(user, game, session, cmd)
      case _                         => InvalidInput(user, "unknown command")
    }

  private def splitFirstWord(text: String): (String, String) = {
    val trimmedText = text.trim
    val firstSpace  = trimmedText.indexOf(' ')
    if (firstSpace < 0)
      (trimmedText, "")
    else
      (trimmedText.substring(0, firstSpace), trimmedText.substring(firstSpace + 1).trim)
  }

  private def splitFirstTwoWords(text: String): (String, String, String) = {
    val (first, intermediate) = splitFirstWord(text)
    val (second, rest)        = splitFirstWord(intermediate)

    (first, second, rest)
  }
}
