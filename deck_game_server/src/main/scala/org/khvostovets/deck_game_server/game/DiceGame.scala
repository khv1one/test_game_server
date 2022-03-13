package org.khvostovets.deck_game_server.game

import java.util.UUID

case class DiceGame(
  uuid: UUID = DiceGame.uuid,
  name: String = DiceGame.name,
  lobbySize: Int =  DiceGame.lobbySize
) extends Game {

}

object DiceGame extends Game {
  implicit val uuid: UUID = UUID.randomUUID()
  implicit val name: String = "dice"

  implicit val lobbySize: Int =  2
}


