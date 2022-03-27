package org.khvostovets.gameserver.game.card

case class GameScore(
  draw: Int,
  folderLoser: Int,
  folderWinner: Int,
  loser: Int,
  winner: Int
)
