package org.khvostovets.gameserver.game

import java.util.UUID

trait Game {
  def uuid: UUID
  def name: String
  def lobbySize: Int
}
