package org.khvostovets.deck_game_server

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps, toTraverseOps}
import org.khvostovets.deck_game_server.game.Game
import org.khvostovets.deck_game_server.message._
import org.khvostovets.deck_game_server.repo.SessionRepoAlg
import org.typelevel.log4cats.Logger

import java.util.UUID


class GameMessageHandler[F[_] : Async, +T <: Game](
  gameLobby: GameLobby[F, T],
  sessionRepo: SessionRepoAlg[F, T]
)(implicit L: Logger[F]) {

  def handle(msg: InputMessage): F[Seq[OutputMessage]] = msg match {
    case EnterToLobby(_, game) =>
      userToLobby(msg.user).map(SendToUser(msg.user, s"You has been added to game $game queue") +: _)

    case UsersInSession(user, _, sessionId) =>
      val messages: F[OutputMessage] = sessionRepo
        .getBySessionId(UUID.fromString(sessionId))
        .map(
          _.map(session => SendToUser(user, (session.users.toSet - user + s"$user(you)").mkString("Users:\n", "\n", "")))
            .getOrElse(SendToUser(user, s"Game session $sessionId not found"))
        )

      messages.map(Seq(_))

    case Disconnect(user) =>
      sessionRepo.getByUserId(user)
        .flatMap { sessions =>
          sessions.toSeq.flatTraverse { session =>
            sessionRepo
              .removeBySessionId(session.uuid)
              .map { _ =>
                val users = session
                  .users
                  .filter(_ != user)

                Seq(SendToUsers(users.toSet, s"Game session ${session.uuid} was canceled because $user disconnected"))
              }
          }
        }

    case _ =>
      Seq.empty[OutputMessage].pure[F]
  }

  private def userToLobby(user: String): F[Seq[OutputMessage]] = {
    gameLobby
      .enqueueUser(user)
      .flatMap { sessionO =>
        sessionO.map { session =>
          sessionRepo.add(session).map { _ =>
            val messages: Seq[OutputMessage] = session
              .users
              .map(user => SendToUser(user, s"you has been add to game, sessionID: ${session.uuid}"))

            messages
          }
        }
          .getOrElse(Seq.empty[OutputMessage].pure[F])
      }
  }
}

object GameMessageHandler {
  def apply[F[_] : Async, T <: Game](lobbySize: Int)(implicit L: Logger[F]): GameMessageHandler[F, T] = {
    new GameMessageHandler[F, T](GameLobby[F, T](lobbySize), SessionRepoAlg.InMemory())
  }
}
