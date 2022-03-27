package org.khvostovets.gameserver

import cats.Parallel
import cats.effect.kernel.Async
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxTuple3Parallel, toFlatMapOps, toFunctorOps, toTraverseOps}
import fs2.Stream
import fs2.concurrent.Topic
import org.http4s.blaze.server.BlazeServerBuilder
import org.khvostovets.config.ConfigHelpers.createConfig
import org.khvostovets.gameserver.config.Config
import org.khvostovets.gameserver.game.card.{OneCardGame, TwoCardGame}
import org.khvostovets.gameserver.game.dice.SimpleDiceGame
import org.khvostovets.gameserver.message.{Disconnect, InputMessage, LobbyMessage, OutputMessage}
import org.khvostovets.gameserver.system.handlers.{CommonHandler, GameHandler}
import org.khvostovets.user.UserRepoAlg
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}

object ServerApplication extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {

    implicit val conf: Config = createConfig[Config]()
    implicit def logger[F]: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    init[IO]
  }

  def init[F[_] : Async : Parallel](implicit L: Logger[F], config: Config): F[ExitCode] = {
    (
      UserRepoAlg.InMemory[F](),
      Topic[F, InputMessage],
      Topic[F, OutputMessage],
    ).parTupled.flatMap { case (userRepo, inputTopic, outputTopic) =>
      Games.init(userRepo).flatMap { games =>
        val commonProcessor = CommonHandler(games.keys)

        val httpStream = Server.httpStream[F](inputTopic, outputTopic, userRepo)
        val processingStream = {
          inputTopic.subscribe(1000)
            .evalMap {
              case msg: Disconnect =>
                games
                  .values
                  .toSeq
                  .traverse {
                    case h: GameHandler.OneCard[F] => h.handle(msg)
                    case h: GameHandler.TwoCard[F] => h.handle(msg)
                    case h: GameHandler.SimpleDice[F] => h.handle(msg)
                  }
                  .map(_.flatten)

              case msg: LobbyMessage =>
                games
                  .get(msg.game)
                  .map {
                    case h: GameHandler.OneCard[F] => h.handle(msg)
                    case h: GameHandler.TwoCard[F] => h.handle(msg)
                    case h: GameHandler.SimpleDice[F] => h.handle(msg)
                  }
                  .getOrElse(Seq.empty[OutputMessage].pure[F])

              case msg =>
                commonProcessor.handle(msg).pure[F]
            }
            .flatMap(Stream.emits(_))
            .through(outputTopic.publish)
        }

        Stream(httpStream, processingStream)
          .parJoinUnbounded
          .compile
          .drain
          .as(ExitCode.Success)
      }
    }
  }
}

object Games {
  def init[F[_] : Async : Parallel](
    userRepo: UserRepoAlg[F]
  ) = {
    (
      GameHandler.OneCard[F](userRepo, 2),
      GameHandler.TwoCard[F](userRepo, 2),
      GameHandler.SimpleDice[F](userRepo, 2)
    ).parTupled.map { case (oneCardHandler, twoCardGame, diceGame) =>
      Map(
        OneCardGame.static.name -> oneCardHandler,
        TwoCardGame.static.name -> twoCardGame,
        SimpleDiceGame.static.name -> diceGame
      )
    }
  }
}

object Server {
  def httpStream[F[_] : Async](
    inputTopic: Topic[F, InputMessage],
    outputTopic: Topic[F, OutputMessage],
    userRepo: UserRepoAlg[F]
  ) (implicit config: Config, L: Logger[F]): Stream[F, ExitCode] = {

    BlazeServerBuilder[F]
      .bindHttp(config.server.port, config.server.host)
      .withHttpWebSocketApp(new ServerRoutes[F](inputTopic, outputTopic, userRepo).wsRoutes(_).orNotFound)
      .serve
  }
}

