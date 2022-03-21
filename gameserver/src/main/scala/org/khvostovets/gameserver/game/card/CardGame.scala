package org.khvostovets.gameserver.game.card

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFoldableOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game._
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}

import java.util.UUID

case class CardGame[F[_] : Async](
  users: NonEmptyList[String],
  usersCards: Map[String, Deck[F]],
  userDecisions: Map[String, GameAction],
  cardHandSize: Int = 1
) {

  def next: GameAction => F[(CardGame[F], Seq[GameResult], Seq[OutputMessage])] = {
    case action : Play => process(action)
    case action : Fold => process(action)
    case action =>
      val messages: Seq[OutputMessage] = Seq(SendToUser(action.user, "Unknown command"))

      (this, Seq.empty[GameResult], messages).pure[F]
  }

  private def process(action: GameAction): F[(CardGame[F], Seq[GameResult], Seq[OutputMessage])] = {
    val (game, playMessages) = play(action)

    game.isEnd.map { case (game, results, messages) => (game, results, messages ++ playMessages)}
  }

  private def play(action: GameAction): (CardGame[F], Seq[SendToUser]) = {
    userDecisions
      .get(action.user)
      .fold {
        (
          new CardGame(users, usersCards, userDecisions + (action.user -> action), cardHandSize), //TODO
          //copy(userDecisions = userDecisions + (action.user -> action)),
          msgToUserAndOther(action.user, "You chose to play", s"Player ${action.user} has made a decision")
        )
      } { _ =>
        (this, Seq(SendToUser(action.user, "You can't change your mind")))
      }
  }

  private def isEnd: F[(CardGame[F], Seq[GameResult], Seq[OutputMessage])] = {
    if (userDecisions.keys.size >= users.size) {
      winnerCalculation().map { case (game, results) =>
        val messages = results.map(result => SendToUser(result.user, result.toString))

        (game, results, messages)
      }
    } else {
      (this, Seq.empty[GameResult], Seq.empty[OutputMessage]).pure[F]
    }
  }

  private def winnerCalculation(): F[(CardGame[F], Seq[GameResult])] = {
    val (players, folders) =
      userDecisions
        .foldLeft(Seq.empty[String], Seq.empty[String]) { case ((players, folders), (user, action)) => action match {
          case _ : Play => (players :+ user, folders)
          case _ : Fold => (players, folders :+ user)
        }}

    if (players.isEmpty) {
      (this, folders.map(user => GameResult(user, -2))).pure[F]
    } else if (players.size == 1) {
      (this, folders.map(user => GameResult(user, -5)) :+ GameResult(players.head, 5)).pure[F]
    } else {
      compareUsersMaxCards(players).flatMap { case (winnerO, losers) =>

        winnerO.fold(
          CardGame[F](NonEmptyList.fromList(losers.toList).get)
            .map { case (game, _) => (game, Seq.empty[GameResult]) } //TODO need to customise message when game restart
        ) { winner =>
          (this, folders.map(user => GameResult(user, -5)) ++ losers.map(user => GameResult(user, -20)) :+ GameResult(winner, 20)).pure[F]
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
}

object CardGame {

  def apply[F[_] : Async](users: NonEmptyList[String]): F[(CardGame[F], Iterable[OutputMessage])] = {
    Deck
      .createDeckOf52[F]()
      .flatMap {
        _.shuffle().flatMap { shuffledDeck =>
          users
            .foldLeftM(Map.empty[String, Deck[F]]) { case (userDecks, user) =>
              shuffledDeck.pullFromTop().flatMap { card =>
                userDecks
                  .get(user)
                  .fold(Deck.empty())(deck => deck.pure[F])
                  .flatMap { userDeck =>
                    userDeck
                      .addToTop(card.get)
                      .map(userDeckWithNewCard => userDecks + (user -> userDeckWithNewCard))
                  }
              }
            }
        }
      }
      .flatMap { usersDeck =>
        val game = new CardGame(users, usersDeck, Map.empty)
        val message = "New game started.\nActions: /play or /fold\n"

        usersDeck
          .toList
          .traverse { case (user, deck) =>
            deck.toStringF.map { deckString =>
              SendToUser(user, message + deckString)
            }
          }
          .map(messages => (game, messages))
      }
  }

  implicit def static[F[_]]: GameStaticInfo[CardGame[F]] = new GameStaticInfo[CardGame[F]] {
    override def uuid: UUID = UUID.randomUUID()
    override def name: String = "card"
  }

  implicit def next[F[_]]: TurnBaseGame[F, CardGame[F]] = new TurnBaseGame[F, CardGame[F]] {
    override def next: CardGame[F] => GameAction => F[(CardGame[F], Seq[GameResult], Seq[OutputMessage])] = { game =>
      action => game.next(action)
    }
  }

  implicit def creator[F[_]: Async]: GameCreator[F, CardGame[F]] = new GameCreator[F, CardGame[F]] {
    override def apply: NonEmptyList[String] => F[(CardGame[F], Iterable[OutputMessage])] = users => CardGame[F](users)
  }
}
