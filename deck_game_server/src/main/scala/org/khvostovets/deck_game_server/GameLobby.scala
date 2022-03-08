package org.khvostovets.deck_game_server

import cats.implicits.catsSyntaxOptionId
import org.khvostovets.deck_game_server.game.Game
import org.khvostovets.user.User

case class GameLobby[+T <: Game](
  users: Set[User] = Set()
) {

  def enqueueUser(user: User): (GameLobby[T], Option[GameSession[T]]) = {
    val newMembers = users + user

    if (newMembers.size >= 2) { // TODO
      val (usersToGame, tail) = newMembers.splitAt(2)
      (copy(tail), GameSession[T](usersToGame.toSeq).some)
    } else {
      (copy(users + user), None)
    }
  }
}

object GameLobby {
  def apply[T <: Game]() = new GameLobby[T]()
}
