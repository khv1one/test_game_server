package org.khvostovets.gameserver.message

import org.khvostovets.user.User
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InputMessageSpec extends AnyWordSpec with Matchers {
  "parsing message" should {
    val user = User("u1")

    "be Help by /help" in {
      val result = MessageParser.parse(user, "/help")

      result mustBe Help(user)
    }

    "be EnterToLobby by /start" in {
      val result = MessageParser.parse(user, "/start    game1   ")

      result mustBe EnterToLobby(user, "game1")
    }

    "be ListGames by /games" in {
      val result = MessageParser.parse(user, " /games  ")

      result mustBe ListGames(user)
    }

    "be UsersInSession by /users" in {
      val result = MessageParser.parse(user, " /users  game1 sessionId   ")

      result mustBe UsersInSession(user, "game1", "sessionId")
    }

    "be GameActionMessage by /{action}" in {
      val result = MessageParser.parse(user, " /play  game1  sessionId     ")

      result mustBe GameActionMessage(user, "game1", "sessionId", "play")
    }

    "be InvalidInput by something else" in {
      val result = MessageParser.parse(user, "error input")

      result mustBe InvalidInput(user, "unknown command")
    }
  }
}
