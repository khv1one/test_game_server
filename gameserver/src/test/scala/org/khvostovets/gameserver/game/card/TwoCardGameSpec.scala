package org.khvostovets.gameserver.game.card

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.implicits.toTraverseOps
import org.khvostovets.gameserver.game.card.deck._
import org.khvostovets.gameserver.game.{Fold, GameResult, Play}
import org.khvostovets.user.User
import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TwoCardGameSpec extends AnyWordSpec with Matchers with PrivateMethodTester {
  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  "Init Game" should {
    "with 10 players" in {
      val playersList = for { i <- 1 to 10 } yield { User(s"u$i") }
      val players = NonEmptyList.fromList(playersList.toList).get

      val (game, messages) = TwoCardGame[IO](players).unsafeRunSync()

      game.userDecisions.isEmpty mustBe true
      game.usersCards.values.toList.flatTraverse(deck => deck.cards.get).unsafeRunSync().toSet.size mustBe 20
      messages.size mustBe 10
    }
  }

  "Next state" should {
    val players = NonEmptyList.of(User("u1"), User("u2"))
    val (game, _) = TwoCardGame[IO](players).unsafeRunSync()

    "fold / fold" in {
      val (nGame, firstResults, _) = game.next(Fold(players.head)).unsafeRunSync()
      val (_, secondResults, _) = nGame.next(Fold(players.last)).unsafeRunSync()


      firstResults.isEmpty mustBe true
      secondResults.size mustBe 2
      secondResults.contains(GameResult(players.head, -2)) mustBe true
      secondResults.contains(GameResult(players.last, -2)) mustBe true
    }

    "play / fold" in {
      val (nGame, firstResults, _) = game.next(Play(players.head)).unsafeRunSync()
      val (_, secondResults, _) = nGame.next(Fold(players.last)).unsafeRunSync()


      firstResults.isEmpty mustBe true
      secondResults.size mustBe 2
      secondResults.contains(GameResult(players.head, 5)) mustBe true
      secondResults.contains(GameResult(players.last, -5)) mustBe true
    }

    "play / play with user2 winner" in {
      val user1Deck = Deck[IO](List(Card(Two(), Diamond), Card(Two(), Spade))).unsafeRunSync()
      val user2Deck = Deck[IO](List(Card(Two(), Club), Card(Three(), Diamond))).unsafeRunSync()

      val gameWithFixWinner = game.copy(usersCards = Map(players.head -> user1Deck, players.last -> user2Deck))

      val (nGameWithFixWinner, firstResults, _) = gameWithFixWinner.next(Play(players.head)).unsafeRunSync()
      val (_, secondResults, _) = nGameWithFixWinner.next(Play(players.last)).unsafeRunSync()

      firstResults.isEmpty mustBe true
      secondResults.size mustBe 2
      secondResults.contains(GameResult(players.head, -20)) mustBe true
      secondResults.contains(GameResult(players.last, 20)) mustBe true
    }

    "play / play with draw and winning with first card" in {
      val user1Deck1 = Deck[IO](List(Card(Ace(), Diamond), Card(Two(), Diamond))).unsafeRunSync()
      val user2Deck1 = Deck[IO](List(Card(Ace(), Club), Card(Two(), Club))).unsafeRunSync()
      val user1Deck2 = Deck[IO](List(Card(Seven(), Diamond), Card(King(), Diamond))).unsafeRunSync()
      val user2Deck2 = Deck[IO](List(Card(Two(), Club), Card(Ten(), Diamond))).unsafeRunSync()

      val gameWithFixWinner = game.copy(usersCards = Map(players.head -> user1Deck1, players.last -> user2Deck1))

      val (nGameWithFixWinner, firstResults, _) = gameWithFixWinner.next(Play(players.head)).unsafeRunSync()
      val (restartedGame, secondResults, _) = nGameWithFixWinner.next(Play(players.last)).unsafeRunSync()

      val restartedGameWithFixWinner = restartedGame.copy(usersCards = Map(players.head -> user1Deck2, players.last -> user2Deck2))

      val (nRestartedGame, thirdResult, _) = restartedGameWithFixWinner.next(Play(players.head)).unsafeRunSync()
      val (_, fourthResult, _) = nRestartedGame.next(Play(players.last)).unsafeRunSync()

      firstResults.isEmpty mustBe true
      secondResults.isEmpty mustBe true
      thirdResult.isEmpty mustBe true
      fourthResult.size mustBe 2

      fourthResult.contains(GameResult(players.head, 20)) mustBe true
      fourthResult.contains(GameResult(players.last, -20)) mustBe true
    }

    "play / play with draw and winning with second card" in {
      val user1Deck1 = Deck[IO](List(Card(Ace(), Diamond), Card(Two(), Diamond))).unsafeRunSync()
      val user2Deck1 = Deck[IO](List(Card(Ace(), Club), Card(Two(), Club))).unsafeRunSync()
      val user1Deck2 = Deck[IO](List(Card(King(), Diamond), Card(Two(), Diamond))).unsafeRunSync()
      val user2Deck2 = Deck[IO](List(Card(King(), Club), Card(Three(), Diamond))).unsafeRunSync()

      val gameWithFixWinner = game.copy(usersCards = Map(players.head -> user1Deck1, players.last -> user2Deck1))

      val (nGameWithFixWinner, firstResults, _) = gameWithFixWinner.next(Play(players.head)).unsafeRunSync()
      val (restartedGame, secondResults, _) = nGameWithFixWinner.next(Play(players.last)).unsafeRunSync()

      val restartedGameWithFixWinner = restartedGame.copy(usersCards = Map(players.head -> user1Deck2, players.last -> user2Deck2))

      val (nRestartedGame, thirdResult, _) = restartedGameWithFixWinner.next(Play(players.head)).unsafeRunSync()
      val (_, fourthResult, _) = nRestartedGame.next(Play(players.last)).unsafeRunSync()

      firstResults.isEmpty mustBe true
      secondResults.isEmpty mustBe true
      thirdResult.isEmpty mustBe true
      fourthResult.size mustBe 2

      fourthResult.contains(GameResult(players.head, -20)) mustBe true
      fourthResult.contains(GameResult(players.last, 20)) mustBe true
    }
  }
}
