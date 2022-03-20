package org.khvostovets.gameserver.game

case class GameResult(
  user: String,
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
