package org.khvostovets.gameserver

import cats.data.NonEmptyList
import cats.effect.{Async, Ref}
import org.khvostovets.gameserver.game.Game
import org.khvostovets.gameserver.message.{GameAction, OutputMessage, SendToUser, Play, Skip}

import java.util.UUID
import scala.util.Random

case class GameSession[F[_] : Async, +T <: Game](
  uuid: UUID,
  users: NonEmptyList[String],
  gameState: Ref[F, DiceGameState]
) {

  def play(action: GameAction): F[(Boolean, Seq[OutputMessage])] = {
    gameState.modify { state =>
      val (nextState, messages) = state.next(action)

      (nextState, (nextState.isFinish, messages))
    }
  }
}

object GameSession {
  def apply[F[_] : Async, T <: Game](users:
    NonEmptyList[String],
    uuid: UUID = UUID.randomUUID
  ): (GameSession[F, T], Seq[SendToUser]) = {
    val (state, messages) = DiceGameState(users)

    (new GameSession[F, T](uuid, users, Ref.unsafe[F, DiceGameState](state)), messages)
  }
}

case class DiceGameState(
  users: NonEmptyList[String],
  currentUser: String,
  usersScore: Map[String, Int],
  stepCounter: Int = 0,
  isFinish: Boolean = false
) {

  def next(action: GameAction): (DiceGameState, Seq[OutputMessage]) = {
    action match {
      case action: Play => play(action.user)
      case action: Skip => skip(action.user)
    }
  }

  private def play(user: String): (DiceGameState, Seq[OutputMessage]) = {
    val (state, messages) = nextStep()
    val (state1, messages1) = state.nextUser()
    val (state2, messages2) = state1.isEnd()

    (state2, messages ++ messages1 ++ messages2)
  }

  private def skip(user: String): (DiceGameState, Seq[OutputMessage]) = {
    val (state1, messages1) = nextUser()
    val (state2, messages2) = state1.isEnd()

    (state2, messages1 ++ messages2)
  }

  private def nextUser(): (DiceGameState, Seq[SendToUser]) = {
    val nextUsers = NonEmptyList.fromListUnsafe(users.tail :+ users.head)

    (copy(users = nextUsers, currentUser = nextUsers.head), Seq.empty)
  }

  private def nextStep(): (DiceGameState, Seq[SendToUser]) = {
    val score = Random.nextInt()

    (
      copy(usersScore = usersScore + (currentUser -> (usersScore(currentUser) + score))),
      msgToUserAndOther(currentUser, s"You got $score", s"$currentUser got $score")
    )
  }

  private def isEnd(): (DiceGameState, Seq[SendToUser]) = {
    if (stepCounter / users.size >= 2) {
      val (user, _) = usersScore.maxBy { case (_, score) => score }

      (
        copy(isFinish = true, stepCounter = stepCounter + 1),
        msgToUserAndOther(user, "You win!", s"$user win!")
      )
    } else {
      (
        copy(stepCounter = stepCounter + 1),
        msgToUserAndOther(currentUser, "Your turn now!", s"next $currentUser turn!")
      )
    }
  }

  private def msgToUserAndOther(user: String, msg1: String, msg2: String): Seq[SendToUser] = {
    users.filterNot(_ == user).map(user => SendToUser(user, msg2)) :+ SendToUser(user, msg1)
  }

  private def toAllUsers(msg: String): Seq[SendToUser] = {
    users.toList.map(user => SendToUser(user, msg))
  }
}

object DiceGameState {
  def apply(users: NonEmptyList[String]): (DiceGameState, Seq[SendToUser]) = {

    val state = new DiceGameState(users, users.head, users.toList.map(_ -> 0).toMap)
    val message = "New game started.\nActions: play or skip\n"

    (
      state,
      state.msgToUserAndOther(state.currentUser, message + "Your turn!", message + s"${state.currentUser}'s turn!")
    )
  }
}