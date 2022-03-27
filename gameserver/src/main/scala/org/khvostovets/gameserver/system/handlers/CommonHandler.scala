package org.khvostovets.gameserver.system.handlers

import org.khvostovets.gameserver.message._

case class CommonHandler(
  games: Iterable[String]
) {

  def handle(msg: InputMessage): Seq[OutputMessage] = msg match {
    case Help(user) =>
      Seq(SendToUser(user, CommonHandler.HelpText))

    case ListGames(user) =>
      Seq(SendToUser(user, games.mkString("games: \n", "\n", "")))

    case _ =>
      Nil
  }
}

object CommonHandler {
  val HelpText: String =
    """Commands:
      |  /help              - Show help
      |  /start <game name> - Start game
      |  /games             - List of games
    """.stripMargin
}