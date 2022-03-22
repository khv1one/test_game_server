package org.khvostovets.gameserver.game.card

import org.khvostovets.gameserver.game.card.deck.{Ace, Card, Diamond, Jack, King, Queen, Two}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CardSpec extends AnyWordSpec with Matchers {
  "compareByRank" should {

    "Queen is higher then Jack" in {
      val queen = Card(Queen(), Diamond)
      val jack = Card(Jack(), Diamond)

      queen.compareByRank(jack) mustBe 1
    }

    "Two is lower then Ace" in {
      val two = Card(Two(), Diamond)
      val ace = Card(Ace(), Diamond)

      two.compareByRank(ace) mustBe -1
    }

    "King is eq King" in {
      val king = Card(King(), Diamond)

      king.compareByRank(king) mustBe 0
    }
  }
}
