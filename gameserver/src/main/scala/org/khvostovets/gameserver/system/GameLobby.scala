package org.khvostovets.gameserver.system

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game.{Game, GameCreator}
import org.khvostovets.gameserver.message.OutputMessage
import org.khvostovets.gameserver.repo.LobbyRepoAlg
import org.khvostovets.user.User

case class GameLobby[F[_] : Async, T <: Game[F, T]](
  users: LobbyRepoAlg[F, User],
  lobbySize: Int
) {

  def enqueueUser(
    user: User
  )(implicit evc: GameCreator[F, T]): F[Option[(GameSession[F, T], Iterable[OutputMessage])]] = {
    users
      .append(user)
      .flatMap { _ =>
        users.catOff(lobbySize).flatMap { usersHead =>
          if (usersHead.size >= lobbySize) {
            NonEmptyList
              .fromList(usersHead)
              .traverse { users =>
                GameSession[F, T](users)
                  .map { case (session, messages) =>

                    (session, messages)
                  }
              }
          } else {
            Option.empty[(GameSession[F, T], Iterable[OutputMessage])].pure[F]
          }
        }
      }
  }
}

object GameLobby {
  def apply[F[_] : Async, T <: Game[F, T]](lobbySize: Int): F[GameLobby[F, T]] = {
    LobbyRepoAlg.InMemory[F, User]().map(new GameLobby[F, T](_, lobbySize))
  }
}
