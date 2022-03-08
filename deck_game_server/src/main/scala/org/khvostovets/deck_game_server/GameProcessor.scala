package org.khvostovets.deck_game_server

import org.khvostovets.deck_game_server.game.Game
import org.khvostovets.deck_game_server.message.{InputMessage, OutputMessage, SendToUser, UserToLobby}
import org.khvostovets.user.User

import java.util.UUID


case class GameProcessor[+T <: Game]() {
  val gameSessionByUuid: Map[UUID, GameSession[T]] = Map.empty
  val gameLobby: GameLobby[T] = GameLobby[T]()

  def process(msg: InputMessage): (GameProcessor[T], Seq[OutputMessage]) = msg match {
    case UserToLobby(user, uuid) =>
      (this, Seq(SendToUser(user, s"add to game $uuid queue")))

    case _ =>
      (this, Nil)
  }

  private def addSession(users: Seq[User]): Map[UUID, GameSession[T]] = {
    val uuid = UUID.randomUUID()

    gameSessionByUuid + (uuid -> GameSession[T](uuid, users))
  }

  private def removeSession(uuid: UUID): Map[UUID, GameSession[T]] = {
    gameSessionByUuid - uuid
  }

  private def userToLobby(user: User): (GameLobby[T], Map[UUID, GameSession[T]]) = {
    val (lobby, sessionO) = gameLobby.enqueueUser(user)
    val sessions = sessionO.map(session => gameSessionByUuid + (session.uuid -> session)).getOrElse(gameSessionByUuid)

    (lobby, sessions)
  }
}

object GameProcessor {
  def apply[T <: Game](): GameProcessor[T] = new GameProcessor[T]()
}