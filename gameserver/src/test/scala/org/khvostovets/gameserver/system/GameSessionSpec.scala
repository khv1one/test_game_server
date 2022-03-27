package org.khvostovets.gameserver.system

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.khvostovets.gameserver.game.GameAction
import org.khvostovets.gameserver.game.card.OneCardGame
import org.khvostovets.user.User
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GameSessionSpec  extends AnyWordSpec with Matchers {
  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  "Play" should {
    val users = NonEmptyList.of(User("u1"), User("u2"))
    val (session, _) = GameSession[IO, OneCardGame[IO]](users).unsafeRunSync()

    "be update game state" in {
      val oldState = session.gameState.get.unsafeRunSync()

      session.play(GameAction("play", User("u1"))).unsafeRunSync()
      val state = session.gameState.get.unsafeRunSync()

      oldState == state mustBe false
    }
  }
}
