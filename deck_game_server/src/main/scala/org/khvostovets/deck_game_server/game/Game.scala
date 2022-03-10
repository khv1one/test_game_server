package org.khvostovets.deck_game_server.game

import java.util.UUID

trait Game {
  def uuid: UUID
  def name: String
  def lobbySize: Int
}
