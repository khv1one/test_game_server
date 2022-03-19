package org.khvostovets.gameserver.game

import cats.data.NonEmptyList
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}

import java.util.UUID
import scala.util.Random

case class CardGame(
  users: NonEmptyList[String],
  currentUser: String,
  usersScore: Map[String, Int],
  stepCounter: Int = 0,
  isFinish: Boolean = false
) {

  def next: GameAction => (CardGame, Seq[OutputMessage]) = {
    case action: Play => play(action.user)
    case action: Skip => skip(action.user)
  }

  private def play(user: String): (CardGame, Seq[OutputMessage]) = {
    val (state, messages) = nextStep()
    val (state1, messages1) = state.nextUser()
    val (state2, messages2) = state1.isEnd()

    (state2, messages ++ messages1 ++ messages2)
  }

  private def skip(user: String): (CardGame, Seq[OutputMessage]) = {
    val (state1, messages1) = nextUser()
    val (state2, messages2) = state1.isEnd()

    (state2, messages1 ++ messages2)
  }

  private def nextUser(): (CardGame, Seq[SendToUser]) = {
    val nextUsers = NonEmptyList.fromListUnsafe(users.tail :+ users.head)

    (copy(users = nextUsers, currentUser = nextUsers.head), Seq.empty)
  }

  private def nextStep(): (CardGame, Seq[SendToUser]) = {
    val score = Random.nextInt()

    (
      copy(usersScore = usersScore + (currentUser -> (usersScore(currentUser) + score))),
      msgToUserAndOther(currentUser, s"You got $score", s"$currentUser got $score")
    )
  }

  private def isEnd(): (CardGame, Seq[SendToUser]) = {
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
}

object CardGame {

  def apply(users: NonEmptyList[String]): (CardGame, Seq[SendToUser]) = {
    val state = new CardGame(users, users.head, users.toList.map(_ -> 0).toMap)
    val message = "New game started.\nActions: play or skip\n"

    (
      state,
      state.msgToUserAndOther(state.currentUser, message + "Your turn!", message + s"${state.currentUser}'s turn!")
    )
  }

  implicit val staticInfo: GameStaticInfo[CardGame] = new GameStaticInfo[CardGame] {
    override def uuid: UUID = UUID.randomUUID()
    override def name: String = "card"
  }

  implicit val next: TurnBaseGame[CardGame] = new TurnBaseGame[CardGame] {
    override def next: CardGame => GameAction => (CardGame, Seq[OutputMessage]) = { game =>
      action => game.next(action)
    }
  }

  implicit val applyAndSend: GameCreator[CardGame] = new GameCreator[CardGame] {
    override def apply: NonEmptyList[String] => (CardGame, Seq[SendToUser]) = users => CardGame(users)
  }
}
