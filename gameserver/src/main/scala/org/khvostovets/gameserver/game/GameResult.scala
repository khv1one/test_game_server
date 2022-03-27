package org.khvostovets.gameserver.game

import org.khvostovets.user.User

case class GameResult(
  user: User,
  score: Int
) {

  override def toString: String = {
    if (score > 0) {
      s"You won $score points"
    } else if (score < 0) {
      s"You lost ${score.abs} points"
    } else {
      s"Your points not changes"
    }
  }
}
