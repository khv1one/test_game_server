package org.khvostovets.deck_game_server

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits.{catsSyntaxOptionId, toFlatMapOps, toFunctorOps}
import org.khvostovets.deck_game_server.game.Game
import org.khvostovets.deck_game_server.message.OutputMessage
import org.khvostovets.deck_game_server.repo.LobbyRepoAlg

case class GameLobby[F[_] : Async, +T <: Game](
  users: LobbyRepoAlg[F, String],
  lobbySize: Int
) {

  def enqueueUser(user: String): F[Option[(GameSession[F, Game], Seq[OutputMessage])]] = {
    users
      .append(user)
      .flatMap { _ =>
        users.catOff(lobbySize).map { usersHead =>
          if (usersHead.size >= lobbySize) {
            NonEmptyList.fromList(usersHead).map(GameSession[F, T](_))
          } else {
            None
          }
        }
      }
  }
}

object GameLobby {
  def apply[F[_] : Async, T <: Game](lobbySize: Int): GameLobby[F, T] = {
    new GameLobby[F, T](LobbyRepoAlg.InMemory[F, String](), lobbySize)
  }
}
