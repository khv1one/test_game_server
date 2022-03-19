package org.khvostovets.gameserver.system

import cats.data.NonEmptyList
import cats.effect.{Async, Ref}
import org.khvostovets.gameserver.game.{GameAction, GameCreator, TurnBaseGame}
import org.khvostovets.gameserver.message.OutputMessage

import java.util.UUID

case class GameSession[F[_] : Async, T : TurnBaseGame](
  uuid: UUID,
  users: NonEmptyList[String],
  gameState: Ref[F, T]
) {

  def play(action: GameAction): F[(Boolean, Seq[OutputMessage])] = {
    gameState.modify { state =>
      val (nextState, messages) = TurnBaseGame[T].next(state)(action)

      (nextState, (false, messages))
    }
  }
}

object GameSession {
  def apply[F[_] : Async, T : GameCreator : TurnBaseGame](
    users: NonEmptyList[String],
    uuid: UUID = UUID.randomUUID
  ): (GameSession[F, T], Seq[OutputMessage]) = {
    val (state, messages) = GameCreator[T].apply(users)

    (new GameSession(uuid, users, Ref.unsafe[F, T](state)), messages)
  }
}
