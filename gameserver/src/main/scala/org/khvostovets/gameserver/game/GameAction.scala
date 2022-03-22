package org.khvostovets.gameserver.game

sealed trait GameAction {
  def user: String
}

case class Play(user: String) extends GameAction
case class Fold(user: String) extends GameAction
case class Unknown(user: String) extends GameAction

object GameAction {
  def apply(action: String, user: String): GameAction = {
    action match {
      case "play" => Play(user)
      case "fold" => Fold(user)
      case _ => Unknown(user)
    }
  }
}
