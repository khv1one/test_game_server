package org.khvostovets.gameserver.game.dice

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits.catsSyntaxApplicativeId
import org.khvostovets.gameserver.game.card.DecisionApplier
import org.khvostovets.gameserver.game._
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}

import java.util.UUID

case class SimpleDiceGame[F[_] : Async](
  users: NonEmptyList[String],
  userDecisions: Map[String, GameAction]
) (implicit evt: TurnBaseGame[F, SimpleDiceGame[F]]) extends DiceGame[F, SimpleDiceGame[F]](users, userDecisions) {
  override def get: Game[F, SimpleDiceGame[F]] => SimpleDiceGame[F] = SimpleDiceGame.game.get
}

object SimpleDiceGame {
  def apply[F[_] : Async](
    users: NonEmptyList[String]
  )(implicit evt: TurnBaseGame[F, SimpleDiceGame[F]]): F[(SimpleDiceGame[F], Iterable[OutputMessage])] = {
    val game = new SimpleDiceGame[F](users, Map.empty)
    val messages: Iterable[OutputMessage] = users.map(SendToUser(_, "New game started.\nActions: /next\n")).toList

    (game, messages).pure[F]
  }

  implicit def static[F[_]]: GameStaticInfo[SimpleDiceGame[F]] = new GameStaticInfo[SimpleDiceGame[F]] {
    override def uuid: UUID = UUID.randomUUID()
    override def name: String = "dice"
  }

  implicit def creator[F[_]: Async]: GameCreator[F, SimpleDiceGame[F]] = new GameCreator[F, SimpleDiceGame[F]] {
    override def apply: NonEmptyList[String] => F[(SimpleDiceGame[F], Iterable[OutputMessage])] = users => SimpleDiceGame[F](users)
  }

  implicit def game[F[_]]: Game[F, SimpleDiceGame[F]] = new Game[F, SimpleDiceGame[F]] {
    override def get: Game[F, SimpleDiceGame[F]] => SimpleDiceGame[F] = {
      case game: SimpleDiceGame[F] => game
    }
  }

  implicit def turn[F[_]]: TurnBaseGame[F, SimpleDiceGame[F]] = new TurnBaseGame[F, SimpleDiceGame[F]] {
    override def next: SimpleDiceGame[F] => GameAction => F[(SimpleDiceGame[F], Seq[GameResult], Seq[OutputMessage])] = { game =>
      action => game.next(action)
    }
  }

  implicit def update[F[_]: Async]: DecisionApplier[F, SimpleDiceGame[F]] = new DecisionApplier[F, SimpleDiceGame[F]] {
    override def setDecision: (SimpleDiceGame[F], Map[String, GameAction]) => SimpleDiceGame[F] = { case (game, decisions) =>
      game.copy(userDecisions = decisions)
    }
  }
}
