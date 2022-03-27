package org.khvostovets.gameserver.system

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.khvostovets.gameserver.game.card.OneCardGame
import org.khvostovets.user.User
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GameLobbySpec extends AnyWordSpec with Matchers {
  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  "Adding user to lobby" should {
    val lobby = GameLobby[IO, OneCardGame[IO]](4).unsafeRunSync

    "be not create session because not enough users" in {
      lobby.enqueueUser(User("u1")).unsafeRunSync().isEmpty mustBe true
      lobby.enqueueUser(User("u2")).unsafeRunSync().isEmpty mustBe true
      lobby.enqueueUser(User("u3")).unsafeRunSync().isEmpty mustBe true
    }

//    "be not create session because not enough unique users" in {
//      lobby.enqueueUser(User("u1")).unsafeRunSync().isEmpty mustBe true
//    }

    "be create session" in {
      lobby.enqueueUser(User("u4")).unsafeRunSync().isEmpty mustBe false
    }
  }
}
