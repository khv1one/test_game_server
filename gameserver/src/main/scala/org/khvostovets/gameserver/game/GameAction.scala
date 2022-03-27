package org.khvostovets.gameserver.game

import org.khvostovets.user.User

sealed trait GameAction {
  def user: User
}

case class Play(user: User) extends GameAction
case class Fold(user: User) extends GameAction
case class Next(user: User) extends GameAction
case class Unknown(user: User) extends GameAction

object GameAction {
  def apply(action: String, user: User): GameAction = {
    action match {
      case "play" => Play(user)
      case "fold" => Fold(user)
      case "next" => Next(user)
      case _ => Unknown(user)
    }
  }
}
