package org.khvostovets.gameserver.game.card.deck

import cats.effect.{Async, Ref}
import cats.implicits.{catsSyntaxOptionId, toFunctorOps}
import org.khvostovets.gameserver.game.card.deck

import scala.util.Random

case class Deck[F[_] : Async](cards: Ref[F, List[Card]]) {
  def shuffle(): F[Deck[F]] = cards.update(cards => Random.shuffle(cards)).map(_ => this)

  def pullFromTop(): F[Option[Card]] = {
    cards.modify(cards =>
      cards.headOption.fold((cards, Option.empty[Card]))(card => (cards.filterNot(_ == card), card.some))
    )
  }

  def addToTop(card: Card): F[Deck[F]] = cards.update(_ :+ card).map(_ => this)

  def getMaxCard(): F[Option[Card]] = {
    cards
      .modify { cards =>
        cards.maxByOption(card => card.rank.score)
          .fold((cards, Option.empty[Card])) { card =>
            (cards.filterNot(_ == card), card.some)
          }
      }
  }

  def isEmpty: F[Boolean] = cards.get.map(_.isEmpty)

  def toStringF: F[String] = {
    cards.get.map(_.mkString(", \n"))
  }
}

object Deck {

  def apply[F[_] : Async](cards: List[Card]): F[Deck[F]] = {
    Ref.of[F, List[Card]](cards).map(cards => new Deck(cards))
  }

  def apply[F[_] : Async](ranks: List[Rank], suites: List[Suite]): F[Deck[F]] = {
    val cards = for(rank <- ranks; suite <- suites) yield deck.Card(rank, suite)

    Deck(cards)
  }

  def empty[F[_] : Async](): F[Deck[F]] = Deck(List.empty)

  def createDeckOf52[F[_] : Async](): F[Deck[F]] = {
    val cards = for(
      rank <- List(Two(), Three(), Four(), Five(), Six(), Seven(), Eight(), Nine(), Ten(), Jack(), Queen(), King(), Ace());
      suite <- Set(Spade, Heart, Club, Diamond)
    ) yield Card(rank, suite)

    Deck(cards)
  }

  def createDeckOf36[F[_] : Async](): F[Deck[F]] = {
    val cards = for(
      rank <- List(Six(), Seven(), Eight(), Nine(), Ten(), Jack(), Queen(), King(), Ace());
      suite <- Set(Spade, Heart, Club, Diamond)
    ) yield Card(rank, suite)

    Deck(cards)
  }
}