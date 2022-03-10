package org.khvostovets.deck_game_server.game

import java.util.UUID

abstract class DiceGame() extends Game

case object DiceGame extends Game {
  implicit val uuid: UUID = UUID.randomUUID()
  implicit val name: String = "dice"

  implicit val lobbySize: Int =  2
}

