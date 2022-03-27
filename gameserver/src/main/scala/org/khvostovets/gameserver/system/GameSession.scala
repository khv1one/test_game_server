package org.khvostovets.gameserver.system

import cats.data.NonEmptyList
import cats.effect.{Async, Ref}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import org.khvostovets.gameserver.game.{GameAction, GameCreator, GameResult, TurnBaseGame}
import org.khvostovets.gameserver.message.OutputMessage
import org.khvostovets.user.User

import java.util.UUID

case class GameSession[F[_] : Async, T](
  uuid: UUID,
  users: NonEmptyList[User],
  gameState: Ref[F, T]
) {

  def play(action: GameAction)(implicit ev: TurnBaseGame[F, T]): F[(Seq[GameResult], Seq[OutputMessage])] = {

    (for {
      state <- gameState.get
      res <- TurnBaseGame[F, T].next(state)(action)

      (nextState, results, messages) = res
    } yield {
      gameState
        .update(_ => nextState)
        .map(_ => (results, messages))
    })
      .flatten

  }
}

object GameSession {
  def apply[F[_] : Async, T](
    users: NonEmptyList[User],
    uuid: UUID = UUID.randomUUID
  )(implicit evc: GameCreator[F, T]): F[(GameSession[F, T], Iterable[OutputMessage])] = {
    GameCreator[F, T].apply(users).flatMap { case (state, messages) =>
      Ref.of[F, T](state)
        .map(gameState => (new GameSession(uuid, users, gameState), messages))
    }
  }
}
