package org.khvostovets.deck_game_server

import org.khvostovets.deck_game_server.game.Game
import org.khvostovets.user.User

import java.util.UUID

case class GameSession[+T <: Game](
  uuid: UUID,
  users: Seq[User]
)

object GameSession {
  def apply[T <: Game](users: Seq[User], uuid: UUID = UUID.randomUUID) = new GameSession[T](uuid, users)
}
