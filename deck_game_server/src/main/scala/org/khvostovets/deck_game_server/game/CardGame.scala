package org.khvostovets.deck_game_server.game

import java.util.UUID

case class CardGame(
  uuid: UUID = CardGame.uuid,
  name: String = CardGame.name
) extends Game

object CardGame extends Game {
  implicit val uuid: UUID = UUID.randomUUID()
  implicit val name: String = "Card Game"
}
