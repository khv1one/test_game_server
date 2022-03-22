package org.khvostovets.gameserver.game.card.deck

sealed trait Suite
case object Spade extends Suite
case object Heart extends Suite
case object Club extends Suite
case object Diamond extends Suite

sealed abstract class Rank(pScore: Int) {
  val score: Int = pScore
}
case class Two(pScore: Int = 2) extends Rank(pScore)
case class Three(pScore: Int = 3) extends Rank(pScore)
case class Four(pScore: Int = 4) extends Rank(pScore)
case class Five(pScore: Int = 5) extends Rank(pScore)
case class Six(pScore: Int = 6) extends Rank(pScore)
case class Seven(pScore: Int = 7) extends Rank(pScore)
case class Eight(pScore: Int = 8) extends Rank(pScore)
case class Nine(pScore: Int = 9) extends Rank(pScore)
case class Ten(pScore: Int = 10) extends Rank(pScore)
case class Jack(pScore: Int = 11) extends Rank(pScore)
case class Queen(pScore: Int = 12) extends Rank(pScore)
case class King(pScore: Int = 13) extends Rank(pScore)
case class Ace(pScore: Int = 14) extends Rank(pScore)

case class Card(rank: Rank, suite: Suite) {

  def compareByRank(card: Card): Int = {
    val thisRank = rank.score
    val anotherRank = card.rank.score

    if (thisRank > anotherRank) 1 else if (thisRank < anotherRank) -1 else 0
  }

  override def toString: String = {
    s"$suite, $rank"
  }
}
