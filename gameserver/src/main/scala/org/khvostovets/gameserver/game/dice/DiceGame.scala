package org.khvostovets.gameserver.game.dice

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId, toFunctorOps}
import org.khvostovets.gameserver.game._
import org.khvostovets.gameserver.game.card.DecisionApplier
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}

abstract class DiceGame[F[_] : Async, T <: DiceGame[F, T]](
  users: NonEmptyList[String],
  userDecisions: Map[String, GameAction],
)(implicit evc: GameCreator[F, T], evt: TurnBaseGame[F, T], evg: Game[F, T], dsa: DecisionApplier[F, T]) extends Game[F, T] {

  def next: GameAction => F[(T, Seq[GameResult], Seq[OutputMessage])] = {
    case action : Next => process(action)
    case action =>
      val messages: Seq[OutputMessage] = Seq(SendToUser(action.user, "Unknown command"))

      (get(this), Seq.empty[GameResult], messages).pure[F]
  }

  private def process(action: GameAction): F[(T, Seq[GameResult], Seq[OutputMessage])] = {
    val (game, playMessages) = play(action)

    game.isEnd.map { case (game, results, messages) => (game, results, messages ++ playMessages)}
  }

  private def play(action: GameAction): (T, Seq[SendToUser]) = {
    userDecisions
      .get(action.user)
      .fold {
        (
          DecisionApplier[F, T].setDecision(get(this), userDecisions + (action.user -> action)),
          msgToUserAndOther(action.user, "You chose to play", s"Player ${action.user} has made a decision")
        )
      } { _ =>
        (get(this), Seq(SendToUser(action.user, "You can't change your mind")))
      }
  }

  private def isEnd: F[(T, Seq[GameResult], Seq[OutputMessage])] = {
    if (userDecisions.keys.size >= users.size) {
      winnerCalculation().map { case (game, results) =>
        val messages = results.map(result => SendToUser(result.user, result.toString))

        (game, results, messages)
      }
    } else {
      (get(this), Seq.empty[GameResult], Seq.empty[OutputMessage]).pure[F]
    }
  }

  private def winnerCalculation(): F[(T, Seq[GameResult])] = {
    compareUsersMaxCards(users.toList).map { case (winner, another) =>
      val results = another.map(GameResult(_, -5))

      (get(this), winner.fold(results)(winner => results :+ GameResult(winner, 5)))
    }
  }

  private def compareUsersMaxCards(users: Seq[String]): F[(Option[String], Seq[String])] = {
    users match {
      case head :: tail =>
        val ts: Seq[String] = tail
        (head.some, ts).pure[F]

      case _ => (Option.empty[String], Seq.empty[String]).pure[F]
    }
  }

  private def msgToUserAndOther(user: String, messageToUser: String, messageToAnotherUsers: String): Seq[SendToUser] = {
    users.filterNot(_ == user).map(user => SendToUser(user, messageToAnotherUsers)) :+ SendToUser(user, messageToUser)
  }

  override def get: Game[F, T] => T = Game[F, T].get
}
