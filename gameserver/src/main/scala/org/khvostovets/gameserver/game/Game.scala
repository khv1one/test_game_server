package org.khvostovets.gameserver.game

import cats.data.NonEmptyList
import org.khvostovets.gameserver.message.OutputMessage

import java.util.UUID

trait GameCreator[F[_], T] {
  def apply: NonEmptyList[String] => F[(T, Iterable[OutputMessage])]
}

object GameCreator {
  def apply[F[_], T](implicit ev: GameCreator[F, T]): GameCreator[F, T] = ev
}

trait GameStaticInfo[T] {
  def uuid: UUID
  def name: String
}

object GameStaticInfo {
  def apply[T](implicit ev: GameStaticInfo[T]): GameStaticInfo[T] = ev
}

trait TurnBaseGame[F[_], T] { self =>
  def next: T => GameAction => F[(T, Seq[GameResult], Seq[OutputMessage])]
}

object TurnBaseGame {
  def apply[F[_], T](implicit ev: TurnBaseGame[F, T]): TurnBaseGame[F, T] = ev
}