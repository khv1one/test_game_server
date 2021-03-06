package org.khvostovets.gameserver.game.card

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFoldableOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game.card.deck.Deck
import org.khvostovets.gameserver.game._
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}
import org.khvostovets.user.User

import java.util.UUID

case class OneCardGame[F[_] : Async](
  users: NonEmptyList[User],
  usersCards: Map[User, Deck[F]],
  userDecisions: Map[User, GameAction],
  cardHandSize: Int = OneCardGame.cardHandSize
) extends CardGame[F, OneCardGame[F]](users, usersCards, userDecisions, cardHandSize) {
  override def get: Game[F, OneCardGame[F]] => OneCardGame[F] = _ => this

  override val scores: GameScore = GameScore(-1, -3, 3, -10, 10)
}

object OneCardGame {
  val cardHandSize = 1

  def apply[F[_] : Async](
    users: NonEmptyList[User]
  ): F[(OneCardGame[F], Iterable[OutputMessage])] = {
    Deck
      .createDeckOf52[F]()
      .flatMap {
        _.shuffle().flatMap { shuffledDeck =>
          users
            .foldLeftM(Map.empty[User, Deck[F]]) { case (userDecks, user) =>
              shuffledDeck.pullFromTop().flatMap { card =>
                userDecks
                  .get(user)
                  .fold(Deck.empty())(deck => deck.pure[F])
                  .flatMap { userDeck =>
                    userDeck
                      .addToTop(card.get)
                      .map(userDeckWithNewCard => userDecks + (user -> userDeckWithNewCard))
                  }
              }
            }
        }
      }
      .flatMap { usersDeck =>
        val game = new OneCardGame[F](users, usersDeck, Map.empty, cardHandSize)
        val message = "New game started.\nActions: /play or /fold\n"

        usersDeck
          .toList
          .traverse { case (user, deck) =>
            deck.toStringF.map { deckString =>
              SendToUser(user, message + deckString)
            }
          }
          .map(messages => (game, messages))
      }
  }

  implicit def static[F[_]]: GameStaticInfo[OneCardGame[F]] = new GameStaticInfo[OneCardGame[F]] {
    override def uuid: UUID = UUID.randomUUID()
    override def name: String = "onecard"
  }

  implicit def creator[F[_]: Async]: GameCreator[F, OneCardGame[F]] = new GameCreator[F, OneCardGame[F]] {
    override def apply: NonEmptyList[User] => F[(OneCardGame[F], Iterable[OutputMessage])] = users => OneCardGame[F](users)
  }

  implicit def turn[F[_]]: TurnBaseGame[F, OneCardGame[F]] = new TurnBaseGame[F, OneCardGame[F]] {
    override def next: OneCardGame[F] => GameAction => F[(OneCardGame[F], Seq[GameResult], Seq[OutputMessage])] = { game =>
      action => game.next(action)
    }
  }

  implicit def update[F[_]: Async]: DecisionApplier[F, OneCardGame[F]] = new DecisionApplier[F, OneCardGame[F]] {
    override def setDecision: (OneCardGame[F], Map[User, GameAction]) => OneCardGame[F] = { case (game, decisions) =>
      game.copy(userDecisions = decisions)
    }
  }
}