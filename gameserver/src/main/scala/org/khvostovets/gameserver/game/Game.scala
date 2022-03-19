package org.khvostovets.gameserver.game

import cats.data.NonEmptyList
import org.khvostovets.gameserver.message.OutputMessage

import java.util.UUID

trait GameCreator[T] {
  def apply: NonEmptyList[String] => (T, Seq[OutputMessage])
}

object GameCreator {
  def apply[T](implicit ev: GameCreator[T]): GameCreator[T] = ev
}

trait GameStaticInfo[T] {
  def uuid: UUID
  def name: String
}

object GameStaticInfo {
  def apply[T](implicit ev: GameStaticInfo[T]): GameStaticInfo[T] = ev
}

trait TurnBaseGame[T] {
  def next: T => GameAction => (T, Seq[OutputMessage])
}

object TurnBaseGame {
  def apply[T](implicit ev: TurnBaseGame[T]): TurnBaseGame[T] = ev
}