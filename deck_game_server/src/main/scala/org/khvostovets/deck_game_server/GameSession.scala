package org.khvostovets.deck_game_server

import org.khvostovets.deck_game_server.game.Game

import java.util.UUID

case class GameSession[F[_], +T <: Game](
  uuid: UUID,
  users: Seq[String]
)

object GameSession {
  def apply[F[_], T <: Game](users: Seq[String], uuid: UUID = UUID.randomUUID) = new GameSession[F, T](uuid, users)
}

