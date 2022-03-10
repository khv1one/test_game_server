package org.khvostovets.deck_game_server.game

import java.util.UUID

abstract class CardGame() extends Game

object CardGame extends Game {
  implicit val uuid: UUID = UUID.randomUUID()
  implicit val name: String = "card1"

  implicit val lobbySize: Int = 2
}
