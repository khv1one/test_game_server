package org.khvostovets.deck_game_server

import org.khvostovets.deck_game_server.message.{Help, InputMessage, ListGames, OutputMessage, SendToUser}

case class CommonProcessor(
  games: Iterable[String]
) {

  def process(msg: InputMessage): (CommonProcessor, Seq[OutputMessage]) = msg match {
    case Help(user) =>
      (this, Seq(SendToUser(user, CommonProcessor.HelpText)))

    case ListGames(user) =>
      (this, Seq(SendToUser(user, games.mkString("games: \n", "\n", "."))))

    case _ =>
      (this, Nil)
  }
}

object CommonProcessor {
  val HelpText: String =
    """Commands:
      |  /help              - Show help
      |  /start <game name> - Start game
      |  /games             - List of games
    """.stripMargin
}