package org.khvostovets.gameserver.game.card

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game._
import org.khvostovets.gameserver.game.card.deck.Deck
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}


class CardGame[F[_] : Async, T <: CardGame[F, T]](
  users: NonEmptyList[String],
  usersCards: Map[String, Deck[F]],
  userDecisions: Map[String, GameAction],
  cardHandSize: Int
)(implicit evc: GameCreator[F, T], evt: TurnBaseGame[F, T], evg: Game[F, T]) extends Game[F, T] {

  def next: GameAction => F[(T, Seq[GameResult], Seq[OutputMessage])] = {
    case action : Play => process(action)
    case action : Fold => process(action)
    case action =>
      val messages: Seq[OutputMessage] = Seq(SendToUser(action.user, "Unknown command"))

      (get(this), Seq.empty[GameResult], messages).pure[F]
  }

  private def process(action: GameAction): F[(T, Seq[GameResult], Seq[OutputMessage])] = {
    val (game, playMessages) = play(action)

    game.isEnd.map { case (game, results, messages) => (game, results, messages ++ playMessages)}
  }

  private def play(action: GameAction): (T, Seq[SendToUser]) = {
    userDecisions
      .get(action.user)
      .fold {
        (
          get(new CardGame(users, usersCards, userDecisions + (action.user -> action), cardHandSize)),
          msgToUserAndOther(action.user, "You chose to play", s"Player ${action.user} has made a decision")
        )
      } { _ =>
        (get(this), Seq(SendToUser(action.user, "You can't change your mind")))
      }
  }

  private def isEnd: F[(T, Seq[GameResult], Seq[OutputMessage])] = {
    if (userDecisions.keys.size >= users.size) {
      winnerCalculation().map { case (game, results) =>
        val messages = results.map(result => SendToUser(result.user, result.toString))

        (game, results, messages)
      }
    } else {
      (get(this), Seq.empty[GameResult], Seq.empty[OutputMessage]).pure[F]
    }
  }

  private def winnerCalculation(): F[(T, Seq[GameResult])] = {
    val (players, folders) =
      userDecisions
        .foldLeft(Seq.empty[String], Seq.empty[String]) { case ((players, folders), (user, action)) => action match {
          case _ : Play => (players :+ user, folders)
          case _ : Fold => (players, folders :+ user)
        }}

    if (players.isEmpty) {
      (get(this), folders.map(user => GameResult(user, -2))).pure[F]
    } else if (players.size == 1) {
      (get(this), folders.map(user => GameResult(user, -5)) :+ GameResult(players.head, 5)).pure[F]
    } else {
      compareUsersMaxCards(players).flatMap { case (winnerO, losers) =>

        winnerO.fold(
          GameCreator[F, T].apply(NonEmptyList.fromList(losers.toList).get)
            .map { case (game, _) => (get(game), Seq.empty[GameResult]) } //TODO need to customise message when game restart
        ) { winner =>
          (get(this), folders.map(user => GameResult(user, -5)) ++ losers.map(user => GameResult(user, -20)) :+ GameResult(winner, 20)).pure[F]
        }
      }
    }
  }

  private def compareUsersMaxCards(users: Seq[String], countAcc: Int = 0): F[(Option[String], Seq[String])] = {
    if (countAcc < cardHandSize) {
      usersCards
        .filter { case (key, _) => users.contains(key)}
        .toList
        .traverse { case (user, deck) =>
          deck.getMaxCard()
            .map(cardO => (user, cardO.map(_.rank.score).getOrElse(0))) //TODO really? getOrElse(0)?
        }
        .flatMap { scoreByUser =>
          val (_, usersWithMaxScores) = scoreByUser.groupBy { case (_, scores) => scores }
            .map { case (score, usersWithScore) => (score, usersWithScore.map { case (user, _) => user }) }
            .toList
            .maxBy { case (score, _) => score }

          if (usersWithMaxScores.size > 1) {
            compareUsersMaxCards(usersWithMaxScores, countAcc + 1)
          } else if (usersWithMaxScores.size == 1) {
            (usersWithMaxScores.headOption, users.filterNot(_ == usersWithMaxScores.head)).pure[F]
          } else {
            (Option.empty[String], Seq.empty[String]).pure[F] //TODO something went wrong
          }
        }
    } else {
      (Option.empty[String], users).pure[F]
    }
  }

  private def msgToUserAndOther(user: String, messageToUser: String, messageToAnotherUsers: String): Seq[SendToUser] = {
    users.filterNot(_ == user).map(user => SendToUser(user, messageToAnotherUsers)) :+ SendToUser(user, messageToUser)
  }

  override def get: Game[F, T] => T = Game[F, T].get
}

object CardGame {
  //TODO deck for games constructors
}
