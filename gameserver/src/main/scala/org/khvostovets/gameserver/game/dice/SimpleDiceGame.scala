package org.khvostovets.gameserver.game.dice

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits.catsSyntaxApplicativeId
import org.khvostovets.gameserver.game.card.DecisionApplier
import org.khvostovets.gameserver.game._
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}
import org.khvostovets.user.User

import java.util.UUID

case class SimpleDiceGame[F[_] : Async](
  users: NonEmptyList[User],
  userDecisions: Map[User, GameAction]
) extends DiceGame[F, SimpleDiceGame[F]](users, userDecisions) {
  override def get: Game[F, SimpleDiceGame[F]] => SimpleDiceGame[F] = _ => this
}

object SimpleDiceGame {
  def apply[F[_] : Async](
    users: NonEmptyList[User]
  ): F[(SimpleDiceGame[F], Iterable[OutputMessage])] = {
    val game = new SimpleDiceGame[F](users, Map.empty)
    val messages: Iterable[OutputMessage] = users.map(SendToUser(_, "New game started.\nActions: /next\n")).toList

    (game, messages).pure[F]
  }

  implicit def static[F[_]]: GameStaticInfo[SimpleDiceGame[F]] = new GameStaticInfo[SimpleDiceGame[F]] {
    override def uuid: UUID = UUID.randomUUID()
    override def name: String = "dice"
  }

  implicit def creator[F[_]: Async]: GameCreator[F, SimpleDiceGame[F]] = new GameCreator[F, SimpleDiceGame[F]] {
    override def apply: NonEmptyList[User] => F[(SimpleDiceGame[F], Iterable[OutputMessage])] = users => SimpleDiceGame[F](users)
  }

  implicit def turn[F[_]]: TurnBaseGame[F, SimpleDiceGame[F]] = new TurnBaseGame[F, SimpleDiceGame[F]] {
    override def next: SimpleDiceGame[F] => GameAction => F[(SimpleDiceGame[F], Seq[GameResult], Seq[OutputMessage])] = { game =>
      action => game.next(action)
    }
  }

  implicit def update[F[_]: Async]: DecisionApplier[F, SimpleDiceGame[F]] = new DecisionApplier[F, SimpleDiceGame[F]] {
    override def setDecision: (SimpleDiceGame[F], Map[User, GameAction]) => SimpleDiceGame[F] = { case (game, decisions) =>
      game.copy(userDecisions = decisions)
    }
  }
}
