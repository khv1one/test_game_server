package org.khvostovets.user

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserRepoInMemorySpec extends AnyWordSpec with Matchers {
  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  "adding new user" should {
    val repo = UserRepoAlg.InMemory[IO]().unsafeRunSync
    val user = User("u1")

    "be return true when user unique" in {
      repo.getByName(user.name).unsafeRunSync().isEmpty mustBe true
      repo.addUser(user).unsafeRunSync() mustBe true
    }

    "be return false if user already in repo" in {
      repo.addUser(user).unsafeRunSync() mustBe false
    }
  }

  "getting user" should {
    val repo = UserRepoAlg.InMemory[IO]().unsafeRunSync
    val user = User("u1")

    "be return none if user is not in repo" in {
      repo.getByName(user.name).unsafeRunSync().isEmpty mustBe true
    }

    "be return user if user is in repo" in {
      repo.addUser(user).unsafeRunSync()
      repo.getByName(user.name).unsafeRunSync().contains(user) mustBe true
    }
  }

  "remove user" should {
    val repo = UserRepoAlg.InMemory[IO]().unsafeRunSync
    val user = User("u1")
    repo.addUser(user).unsafeRunSync()

    "be remove user from repo" in {
      repo.removeUser(user).unsafeRunSync()

      repo.getByName(user.name).unsafeRunSync().isEmpty mustBe true
    }
  }

  "changeUserScores" should {
    val repo = UserRepoAlg.InMemory[IO]().unsafeRunSync
    val user = new User("u1", 100)
    repo.addUser(user).unsafeRunSync()

    "edit users tokens in repo" in {
      repo.changeUserScores(user, 40).unsafeRunSync()

      repo.getByName(user.name).unsafeRunSync().get.tokens mustBe 140
    }
  }
}
