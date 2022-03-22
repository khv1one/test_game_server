package org.khvostovets.gameserver.game.card

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFoldableOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game.{Game, GameAction, GameCreator, GameResult, GameStaticInfo, TurnBaseGame}
import org.khvostovets.gameserver.game.card.deck.Deck
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}

import java.util.UUID

case class TwoCardGame[F[_] : Async](
  users: NonEmptyList[String],
  usersCards: Map[String, Deck[F]],
  userDecisions: Map[String, GameAction],
  cardHandSize: Int = TwoCardGame.cardHandSize
) (implicit evt: TurnBaseGame[F, TwoCardGame[F]]) extends CardGame[F, TwoCardGame[F]](users, usersCards, userDecisions, cardHandSize) {
  override def get: Game[F, TwoCardGame[F]] => TwoCardGame[F] = TwoCardGame.game.get
}

object TwoCardGame {
  val cardHandSize = 2

  def apply[F[_] : Async](users: NonEmptyList[String], cardHandSize: Int)(implicit evt: TurnBaseGame[F, TwoCardGame[F]]): F[(TwoCardGame[F], Iterable[OutputMessage])] = {
    Deck
      .createDeckOf52[F]()
      .flatMap {
        _.shuffle().flatMap { shuffledDeck =>
          users
            .foldLeftM(Map.empty[String, Deck[F]]) { case (userDecks, user) =>
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
    override def name: String = "card"
  }

  implicit def creator[F[_]: Async]: GameCreator[F, TwoCardGame[F]] = new GameCreator[F, TwoCardGame[F]] {
    override def apply: NonEmptyList[String] => F[(TwoCardGame[F], Iterable[OutputMessage])] = users => TwoCardGame[F](users, cardHandSize)
  }

  implicit def game[F[_]]: Game[F, TwoCardGame[F]] = new Game[F, TwoCardGame[F]] {
    override def get: Game[F, TwoCardGame[F]] => TwoCardGame[F] = {
      case game: TwoCardGame[F] => game
    }
  }

  implicit def turn[F[_]]: TurnBaseGame[F, TwoCardGame[F]] = new TurnBaseGame[F, TwoCardGame[F]] {
    override def next: TwoCardGame[F] => GameAction => F[(TwoCardGame[F], Seq[GameResult], Seq[OutputMessage])] = { game =>
      action => game.next(action)
    }
  }
}