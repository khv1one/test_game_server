package org.khvostovets.gameserver.game.card

import cats.data.{NonEmptyList, OptionT}
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId, toFlatMapOps, toFoldableOps, toFunctorOps, toTraverseOps}
import org.khvostovets.gameserver.game._
import org.khvostovets.gameserver.message.{OutputMessage, SendToUser}

import java.util.UUID

case class CardGame[F[_] : Async](
  users: NonEmptyList[String],
  usersCards: Map[String, Deck[F]],
  userDecisions: Map[String, GameAction],
  isFinish: Boolean = false
) {

  def next: GameAction => F[(CardGame[F], Seq[GameResult], Seq[OutputMessage])] = {
    case action : Play =>
      val (game, playMessages) = play(action)

      game.isEnd().map { case (results, messages) => (game, results, messages ++ playMessages)}

    case action : Fold =>
      val (game, playMessages) = play(action)

      game.isEnd().map { case (results, messages) => (game, results, messages ++ playMessages)}

    case action =>
      val messages: Seq[OutputMessage] = Seq(SendToUser(action.user, "Unknown command"))

      (this, Seq.empty[GameResult], messages).pure[F]
  }

  private def play(action: GameAction): (CardGame[F], Seq[SendToUser]) = {
    userDecisions
      .get(action.user)
      .fold {
        (
          copy(userDecisions = userDecisions + (action.user -> action)),
          msgToUserAndOther(action.user, "You chose to play", s"Player ${action.user} has made a decision")
        )
      } { _ =>
        (this, Seq(SendToUser(action.user, "You can't change your mind")))
      }
  }

  private def isEnd(): F[(Seq[GameResult], Seq[OutputMessage])] = {
    if (userDecisions.keys.size >= users.size) {
      winnerCalculation().map { results =>
        val messages = results.map(result => SendToUser(result.user, result.toString))

        (results, messages)
      }
    } else {
      (Seq.empty[GameResult], Seq.empty[OutputMessage]).pure[F]
    }
  }

  private def winnerCalculation(): F[Seq[GameResult]] = {
    val (players, folders) =
      userDecisions
        .foldLeft(Seq.empty[String], Seq.empty[String]) { case ((players, folders), (user, action)) => action match {
          case _ : Play => (players :+ user, folders)
          case _ : Fold => (players, folders :+ user)
        }}

    if (players.isEmpty) {
      folders.map(user => GameResult(user, -2)).pure[F]
    } else if (players.size == 1) {
      (folders.map(user => GameResult(user, -5)) :+ GameResult(players.head, 5)).pure[F]
    } else {
      compareDeck(players).map { case (winnerO, losers) =>

        winnerO.fold(
          Seq.empty[GameResult]
        ) { winner =>
          folders.map(user => GameResult(user, -5)) ++ losers.map(user => GameResult(user, -20)) :+ GameResult(winner, 20)
        }
      }
    }
  }

  private def compareDeck(users: Seq[String]): F[(Option[String], Seq[String])] = {
    val usersSet = users.toSet

    usersCards
      .filter { case (key, _) => usersSet.contains(key)}
      .toList
      .foldLeftM(Option.empty[String], Option.empty[Deck[F]]) { case ((userAccO, deckAccO), (user, deck)) =>
        deckAccO.fold { (user.some, deck.some).pure[F] } { deckAcc =>
          (for {
            accCard <- OptionT(deckAcc.getMaxCard())
            card <- OptionT(deck.getMaxCard())
          } yield {
            if (card.compareByRank(accCard) > 0) {
              (user.some, deck.some)
            } else {
              (userAccO, deckAccO)
            }
          })
            .fold((Option.empty[String], Option.empty[Deck[F]])) { a => a}
        }
      }
      .map { case (winnerO, _) =>
        (winnerO, winnerO.fold(users)(winner => (usersSet - winner).toSeq))
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
