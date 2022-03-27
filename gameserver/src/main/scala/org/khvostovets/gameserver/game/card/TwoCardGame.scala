package org.khvostovets.gameserver.game.card

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxTuple2Semigroupal, toFlatMapOps, toFoldableOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game.card.deck.Deck
import org.khvostovets.gameserver.game._
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}
import org.khvostovets.user.User

import java.util.UUID

case class TwoCardGame[F[_] : Async](
  users: NonEmptyList[User],
  usersCards: Map[User, Deck[F]],
  userDecisions: Map[User, GameAction],
  cardHandSize: Int = TwoCardGame.cardHandSize
) extends CardGame[F, TwoCardGame[F]](users, usersCards, userDecisions, cardHandSize) {
  override def get: Game[F, TwoCardGame[F]] => TwoCardGame[F] = _ => this

  override val scores: GameScore = GameScore(-2, -5, 5, -20, 20)
}

object TwoCardGame {
  val cardHandSize = 2

  def apply[F[_] : Async](
    users: NonEmptyList[User]
  ): F[(TwoCardGame[F], Iterable[OutputMessage])] = {
    Deck
      .createDeckOf52[F]()
      .flatMap {
        _.shuffle().flatMap { shuffledDeck =>
          users
            .foldLeftM(Map.empty[User, Deck[F]]) { case (userDecks, user) =>
              (
                shuffledDeck.pullFromTop(),
                shuffledDeck.pullFromTop()
              ).tupled.flatMap { case (card1, card2) =>
                userDecks
                  .get(user)
                  .fold(Deck.empty())(deck => deck.pure[F])
                  .flatMap { userDeck =>
                    userDeck
                      .addToTop(card1.get)
                      .flatMap(_.addToTop(card2.get))
                      .map(userDeckWithNewCard => userDecks + (user -> userDeckWithNewCard))
                  }
              }
            }
        }
      }
      .flatMap { usersDeck =>
        val game = new TwoCardGame[F](users, usersDeck, Map.empty, cardHandSize)
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

  implicit def static[F[_]]: GameStaticInfo[TwoCardGame[F]] = new GameStaticInfo[TwoCardGame[F]] {
    override def uuid: UUID = UUID.randomUUID()
    override def name: String = "twocard"
  }

  implicit def creator[F[_]: Async]: GameCreator[F, TwoCardGame[F]] = new GameCreator[F, TwoCardGame[F]] {
    override def apply: NonEmptyList[User] => F[(TwoCardGame[F], Iterable[OutputMessage])] = users => TwoCardGame[F](users)
  }

  implicit def turn[F[_]]: TurnBaseGame[F, TwoCardGame[F]] = new TurnBaseGame[F, TwoCardGame[F]] {
    override def next: TwoCardGame[F] => GameAction => F[(TwoCardGame[F], Seq[GameResult], Seq[OutputMessage])] = { game =>
      action => game.next(action)  //if we need specific moved we can change this
    }
  }

  implicit def update[F[_] : Async]: DecisionApplier[F, TwoCardGame[F]] = new DecisionApplier[F, TwoCardGame[F]] {
    override def setDecision: (TwoCardGame[F], Map[User, GameAction]) => TwoCardGame[F] = { case (game, decisions) =>
      game.copy(userDecisions = decisions)
    }
  }
}