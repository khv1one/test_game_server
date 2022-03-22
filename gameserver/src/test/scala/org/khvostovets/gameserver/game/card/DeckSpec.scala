package org.khvostovets.gameserver.game.card

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.khvostovets.gameserver.game.card.deck.{Ace, Card, Club, Deck, Diamond, Heart, Spade}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DeckSpec extends AnyWordSpec with Matchers {
  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  "Init Deck" should {

    "Standard deck with 52 cards" in {
      val deck = Deck.createDeckOf52[IO]().unsafeRunSync()
      val cards = deck.cards.get.unsafeRunSync()

       cards.size mustBe 52
       cards.count(_.suite == Spade) mustBe 13
       cards.count(_.suite == Diamond) mustBe 13
       cards.count(_.suite == Heart) mustBe 13
       cards.count(_.suite == Club) mustBe 13
    }

    "Custom cars list" in {
      val cardsForDeck = for(
        rank <- List(Ace());
        suite <- Set(Spade, Diamond)
      ) yield Card(rank, suite)

      val deck = Deck.apply[IO](cardsForDeck).unsafeRunSync()
      val cards = deck.cards.get.unsafeRunSync()


      cards.size mustBe 2
      cards.count(_.suite == Spade) mustBe 1
      cards.count(_.suite == Diamond) mustBe 1
    }
  }

  "Changes Deck" should {
    "Shuffled deck" in {
      val deck = Deck.createDeckOf52[IO]().unsafeRunSync()
      val cards = deck.cards.get.unsafeRunSync()
      val shuffledDeck = deck.shuffle().unsafeRunSync()

      cards must not be shuffledDeck.cards
    }

    "Add card on top" in {
      val deck = Deck.createDeckOf52[IO]().unsafeRunSync()
      val card = deck.pullFromTop().unsafeRunSync()
      val fullDeck = deck.addToTop(card.get).unsafeRunSync()

      fullDeck.cards mustEqual deck.cards
    }

    "Get card from top" in {
      val deck = Deck.createDeckOf52[IO]().unsafeRunSync()
      val card = deck.pullFromTop().unsafeRunSync().get
      val currentCards = deck.cards.get.unsafeRunSync()

      currentCards.size mustEqual 51
      currentCards.filter(_ == card) mustBe List.empty

      Deck.createDeckOf52[IO]().flatMap { deck =>
        for {
          cards1 <- deck.cards.get
          card <- deck.pullFromTop()

          cards2 <- deck.cards.get
        } yield {
          println("cards1: " + cards1.mkString(", "))
          println("card: " + card.toString)
          println("cards2" + cards2.mkString(", "))
        }

      }.unsafeRunSync()
    }

    "Get max card" in {
      val deck = Deck.createDeckOf52[IO]().unsafeRunSync()
      val card = deck.getMaxCard().unsafeRunSync().get
      val cards = deck.cards.get.unsafeRunSync()

      List(Card(Ace(), Diamond), Card(Ace(), Club), Card(Ace(), Heart), Card(Ace(), Spade))
        .contains(card) mustBe true

      cards.size mustBe 51
    }
  }
}
