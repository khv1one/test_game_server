package org.khvostovets.gameserver.system.handlers

import cats.Parallel
import cats.data.OptionT
import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxTuple2Parallel, catsSyntaxTuple2Semigroupal, toFlatMapOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game.card.{OneCardGame, TwoCardGame}
import org.khvostovets.gameserver.game.dice.SimpleDiceGame
import org.khvostovets.gameserver.game.{Game, GameAction, GameCreator, GameResult, TurnBaseGame}
import org.khvostovets.gameserver.message._
import org.khvostovets.gameserver.repo.SessionRepoAlg
import org.khvostovets.gameserver.system.GameLobby
import org.khvostovets.user.{User, UserRepoAlg}

import java.util.UUID

abstract class GameHandler[F[_] : Async, T <: Game[F, T]](
  userRepo: UserRepoAlg[F],
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
          .flatMap(_.map(_.play(GameAction(cmd, user))).getOrElse((Seq.empty[GameResult], Seq.empty[OutputMessage]).pure[F]))
          .flatMap { case (results, messages) =>
            if (results.nonEmpty) {
              (
                sessionRepo.removeBySessionId(UUID.fromString(sessionId)).map(_ => messages),
                results.traverse(result => userRepo.changeUserScores(user, result.score))
              ).tupled.map(_ => messages)
            }
            else {
              messages.pure[F]
            }
          }

      messages

    case Disconnect(user) =>
      sessionRepo.getByUserId(user.name)
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
    user: User
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

object GameHandler {
  case class OneCard[F[_] : Async](
    userRepo: UserRepoAlg[F],
    gameLobby: GameLobby[F, OneCardGame[F]],
    sessionRepo: SessionRepoAlg[F, OneCardGame[F]]
  ) extends GameHandler[F, OneCardGame[F]](userRepo, gameLobby, sessionRepo)

  object OneCard {
    def apply[F[_] : Async : Parallel](
      userRepo: UserRepoAlg[F],
      lobbySize: Int
    ): F[OneCard[F]] = {
      (
        GameLobby[F, OneCardGame[F]](lobbySize),
        SessionRepoAlg.InMemory[F, OneCardGame[F]]()
      ).parTupled.map { case (lobby, sessionRepo) => new OneCard[F](userRepo, lobby, sessionRepo) }
    }
  }

  case class TwoCard[F[_] : Async](
    userRepo: UserRepoAlg[F],
    gameLobby: GameLobby[F, TwoCardGame[F]],
    sessionRepo: SessionRepoAlg[F, TwoCardGame[F]]
  ) extends GameHandler[F, TwoCardGame[F]](userRepo, gameLobby, sessionRepo)

  object TwoCard {
    def apply[F[_] : Async : Parallel](
      userRepo: UserRepoAlg[F],
      lobbySize: Int
    ): F[TwoCard[F]] = {
      (
        GameLobby[F, TwoCardGame[F]](lobbySize),
        SessionRepoAlg.InMemory[F, TwoCardGame[F]]()
      ).parTupled.map { case (lobby, sessionRepo) => new TwoCard[F](userRepo, lobby, sessionRepo) }
    }
  }

  case class SimpleDice[F[_] : Async](
    userRepo: UserRepoAlg[F],
    gameLobby: GameLobby[F, SimpleDiceGame[F]],
    sessionRepo: SessionRepoAlg[F, SimpleDiceGame[F]]
  ) extends GameHandler[F, SimpleDiceGame[F]](userRepo, gameLobby, sessionRepo)

  object SimpleDice {
    def apply[F[_] : Async : Parallel](
      userRepo: UserRepoAlg[F],
      lobbySize: Int
    ): F[SimpleDice[F]] = {
      (
        GameLobby[F, SimpleDiceGame[F]](lobbySize),
        SessionRepoAlg.InMemory[F, SimpleDiceGame[F]]()
      ).parTupled.map { case (lobby, sessionRepo) => new SimpleDice[F](userRepo, lobby, sessionRepo) }
    }
  }
}
