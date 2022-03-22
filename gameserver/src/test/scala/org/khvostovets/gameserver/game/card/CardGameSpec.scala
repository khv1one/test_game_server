package org.khvostovets.gameserver.game.card

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.implicits.toTraverseOps
import org.khvostovets.gameserver.game.{Fold, GameResult, Play}
import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

//class CardGameSpec extends AnyWordSpec with Matchers with PrivateMethodTester {
//  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global
//
//  "Init Game" should {
//    "with 10 players" in {
//      val playersNames = for {i <- 1 to 10} yield s"u$i"
//      val players = NonEmptyList.fromList(playersNames.toList).get
//
//      val (game, messages) = CardGame[IO](players).unsafeRunSync()
//
//      game.userDecisions.isEmpty mustBe true
//      game.usersCards.values.toList.flatTraverse(deck => deck.cards.get).unsafeRunSync().toSet.size mustBe 10
//      messages.size mustBe 10
//    }
//  }
//
//  "Next state" should {
//    val players = NonEmptyList.of("u1", "u2")
//    val (game, _) = CardGame[IO](players).unsafeRunSync()
//
//    "fold / fold" in {
//      val (nGame, firstResults, _) = game.next(Fold("u1")).unsafeRunSync()
//      val (_, secondResults, _) = nGame.next(Fold("u2")).unsafeRunSync()
//
//
//      firstResults.isEmpty mustBe true
//      secondResults.size mustBe 2
//      secondResults.contains(GameResult("u1", -2)) mustBe true
//      secondResults.contains(GameResult("u2", -2)) mustBe true
//    }
//
//    "play / fold" in {
//      val (nGame, firstResults, _) = game.next(Play("u1")).unsafeRunSync()
//      val (_, secondResults, _) = nGame.next(Fold("u2")).unsafeRunSync()
//
//
//      firstResults.isEmpty mustBe true
//      secondResults.size mustBe 2
//      secondResults.contains(GameResult("u1", 5)) mustBe true
//      secondResults.contains(GameResult("u2", -5)) mustBe true
//    }
//
//    "play / play with user2 winner" in {
//      val user1Deck = Deck[IO](List(Card(Two(), Diamond))).unsafeRunSync()
//      val user2Deck = Deck[IO](List(Card(Three(), Club))).unsafeRunSync()
//
//      val gameWithFixWinner = game.copy(usersCards = Map("u1" -> user1Deck, "u2" -> user2Deck))
//
//      val (nGameWithFixWinner, firstResults, _) = gameWithFixWinner.next(Play("u1")).unsafeRunSync()
//      val (_, secondResults, _) = nGameWithFixWinner.next(Play("u2")).unsafeRunSync()
//
//      firstResults.isEmpty mustBe true
//      secondResults.size mustBe 2
//      secondResults.contains(GameResult("u1", -20)) mustBe true
//      secondResults.contains(GameResult("u2", 20)) mustBe true
//    }
//
//    "play / play with draw" in {
//      val user1Deck1 = Deck[IO](List(Card(Ace(), Diamond))).unsafeRunSync()
//      val user2Deck1 = Deck[IO](List(Card(Ace(), Club))).unsafeRunSync()
//      val user1Deck2 = Deck[IO](List(Card(King(), Diamond))).unsafeRunSync()
//      val user2Deck2 = Deck[IO](List(Card(Two(), Club))).unsafeRunSync()
//
//      val gameWithFixWinner = game.copy(usersCards = Map("u1" -> user1Deck1, "u2" -> user2Deck1))
//
//      val (nGameWithFixWinner, firstResults, _) = gameWithFixWinner.next(Play("u1")).unsafeRunSync()
//      val (restartedGame, secondResults, _) = nGameWithFixWinner.next(Play("u2")).unsafeRunSync()
//
//      val restartedGameWithFixWinner = restartedGame.copy(usersCards = Map("u1" -> user1Deck2, "u2" -> user2Deck2))
//
//      val (nRestartedGame, thirdResult, _) = restartedGameWithFixWinner.next(Play("u1")).unsafeRunSync()
//      val (_, fourthResult, _) = nRestartedGame.next(Play("u2")).unsafeRunSync()
//
//      firstResults.isEmpty mustBe true
//      secondResults.isEmpty mustBe true
//      thirdResult.isEmpty mustBe true
//      fourthResult.size mustBe 2
//
//      fourthResult.contains(GameResult("u1", 20)) mustBe true
//      fourthResult.contains(GameResult("u2", -20)) mustBe true
//    }
//  }
//}
