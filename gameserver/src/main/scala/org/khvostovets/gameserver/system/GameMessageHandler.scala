package org.khvostovets.gameserver.system

import cats.Parallel
import cats.data.OptionT
import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxTuple2Parallel, toFlatMapOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game.{Game, GameAction, GameCreator, TurnBaseGame}
import org.khvostovets.gameserver.message._
import org.khvostovets.gameserver.repo.SessionRepoAlg

import java.util.UUID

class GameMessageHandler[F[_] : Async, T <: Game[F, T]](
  gameLobby: GameLobby[F, T],
  sessionRepo: SessionRepoAlg[F, T]
) {

  def handle(
    msg: InputMessage
  )(implicit evt: TurnBaseGame[F, T], evc: GameCreator[F, T]): F[Seq[OutputMessage]] = msg match {
    case EnterToLobby(_, game) =>
      userToLobby(msg.user).map(SendToUser(msg.user, s"You has been added to game $game queue") +: _)

    case UsersInSession(user, _, sessionId) =>
      val messages: F[OutputMessage] =
        OptionT(sessionRepo.getBySessionId(UUID.fromString(sessionId)))
          .map(session => SendToUser(user, session.users.toList.mkString("Users:\n", "\n", "")))
          .getOrElse(SendToUser(user, s"Game session $sessionId not found"))

      messages.map(Seq(_))

    case GameActionMessage(user, _, sessionId, cmd) =>
      val messages: F[Seq[OutputMessage]] =
        sessionRepo
          .getBySessionId(UUID.fromString(sessionId))
          .flatMap(_.map(_.play(GameAction(cmd, user))).getOrElse((false, Seq.empty[OutputMessage]).pure[F]))
          .flatMap { case (isFinish, messages) =>
            if (isFinish) sessionRepo.removeBySessionId(UUID.fromString(sessionId)).map(_ => messages)
            else messages.pure[F]
          }

      messages

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

  private def userToLobby(
    user: String
  )(implicit evc: GameCreator[F, T]): F[Seq[OutputMessage]] = {
    gameLobby
      .enqueueUser(user)
      .flatMap { sessionO =>
        sessionO.map { case (session, sessionMessages) =>
          sessionRepo.add(session).map { _ =>
            val messages: Seq[OutputMessage] = session
              .users
              .toList
              .map(user => SendToUser(user, s"You has been added to game, sessionID: ${session.uuid}"))

            messages ++ sessionMessages
          }
        }
          .getOrElse(Seq.empty[OutputMessage].pure[F])
      }
  }
}

object GameMessageHandler {
  def apply[F[_] : Async : Parallel, T <: Game[F, T]](
    lobbySize: Int
  ): F[GameMessageHandler[F, T]] = {
    (
      GameLobby[F, T](lobbySize),
      SessionRepoAlg.InMemory[F, T]()
    ).parTupled.map { case (lobby, sessionRepo) => new GameMessageHandler[F, T](lobby, sessionRepo) }
  }
}
