package org.khvostovets.gameserver.system

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import org.khvostovets.gameserver.game.{GameCreator, TurnBaseGame}
import org.khvostovets.gameserver.message.OutputMessage
import org.khvostovets.gameserver.repo.LobbyRepoAlg

case class GameLobby[F[_] : Async, T : GameCreator : TurnBaseGame](
  users: LobbyRepoAlg[F, String],
  lobbySize: Int
) {

  def enqueueUser(user: String): F[Option[(GameSession[F, T], Seq[OutputMessage])]] = {
    users
      .append(user)
      .flatMap { _ =>
        users.catOff(lobbySize).map { usersHead =>
          if (usersHead.size >= lobbySize) {
            NonEmptyList
              .fromList(usersHead)
              .map(GameSession[F, T](_))
              .map { case (session, messages) =>

                (session, messages)
              }
          } else {
            None
          }
        }
      }
  }
}

object GameLobby {
  def apply[F[_] : Async, T : GameCreator : TurnBaseGame](lobbySize: Int): GameLobby[F, T] = {
    new GameLobby[F, T](LobbyRepoAlg.InMemory[F, String](), lobbySize)
  }
}
