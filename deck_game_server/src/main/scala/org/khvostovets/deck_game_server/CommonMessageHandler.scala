package org.khvostovets.deck_game_server

import org.khvostovets.deck_game_server.message.{Help, InputMessage, ListGames, OutputMessage, SendToUser}

case class CommonMessageHandler(
  games: Iterable[String]
) {

  def handle(msg: InputMessage): Seq[OutputMessage] = msg match {
    case Help(user) =>
      Seq(SendToUser(user, CommonMessageHandler.HelpText))

    case ListGames(user) =>
      Seq(SendToUser(user, games.mkString("games: \n", "\n", "")))

    case _ =>
      Nil
  }
}

object CommonMessageHandler {
  val HelpText: String =
    """Commands:
      |  /help              - Show help
      |  /start <game name> - Start game
      |  /games             - List of games
    """.stripMargin
}