package org.khvostovets.gameserver.game.card

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFoldableOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game.{Game, GameAction, GameCreator, GameResult, GameStaticInfo, TurnBaseGame}
import org.khvostovets.gameserver.game.card.deck.Deck
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}

import java.util.UUID

case class OneCardGame[F[_] : Async](
  users: NonEmptyList[String],
  usersCards: Map[String, Deck[F]],
  userDecisions: Map[String, GameAction],
  cardHandSize: Int = OneCardGame.cardHandSize
) (implicit evt: TurnBaseGame[F, OneCardGame[F]]) extends CardGame[F, OneCardGame[F]](users, usersCards, userDecisions, cardHandSize) {
  override def get: Game[F, OneCardGame[F]] => OneCardGame[F] = OneCardGame.game.get
}

object OneCardGame {
  val cardHandSize = 1

  def apply[F[_] : Async](users: NonEmptyList[String], cardHandSize: Int)(implicit evt: TurnBaseGame[F, OneCardGame[F]]): F[(OneCardGame[F], Iterable[OutputMessage])] = {
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
    override def name: String = "card"
  }

  implicit def creator[F[_]: Async]: GameCreator[F, OneCardGame[F]] = new GameCreator[F, OneCardGame[F]] {
    override def apply: NonEmptyList[String] => F[(OneCardGame[F], Iterable[OutputMessage])] = users => OneCardGame[F](users, cardHandSize)
  }

  implicit def game[F[_]]: Game[F, OneCardGame[F]] = new Game[F, OneCardGame[F]] {
    override def get: Game[F, OneCardGame[F]] => OneCardGame[F] = {
      case game: OneCardGame[F] => game
    }
  }

  implicit def turn[F[_]]: TurnBaseGame[F, OneCardGame[F]] = new TurnBaseGame[F, OneCardGame[F]] {
    override def next: OneCardGame[F] => GameAction => F[(OneCardGame[F], Seq[GameResult], Seq[OutputMessage])] = { game =>
      action => game.next(action)
    }
  }
}