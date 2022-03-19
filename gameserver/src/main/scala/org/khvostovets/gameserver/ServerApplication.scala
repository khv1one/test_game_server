package org.khvostovets.gameserver

import cats.effect.kernel.Async
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.{catsSyntaxApplicativeId, toTraverseOps}
import fs2.Stream
import fs2.concurrent.Topic
import org.http4s.blaze.server.BlazeServerBuilder
import org.khvostovets.config.ConfigHelpers.createConfig
import org.khvostovets.gameserver.config.Config
import org.khvostovets.gameserver.game.{CardGame, DiceGame, Game}
import org.khvostovets.gameserver.message.{Disconnect, InputMessage, LobbyMessage, OutputMessage}
import org.khvostovets.user.UserRepoAlg
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}

object ServerApplication extends IOApp{
  override def run(args: List[String]): IO[ExitCode] = {

    implicit val conf: Config = createConfig[Config]()
    implicit def logger[F]: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    val userRepo = UserRepoAlg.InMemory[IO]()

    val games = Games.init[IO]
    val commonProcessor = CommonMessageHandler(games.keys)

    for {
      inputTopic <- Topic[IO, InputMessage]
      outputTopic <- Topic[IO, OutputMessage]

      exitCode <- {
        val httpStream = ServerStream.stream[IO](inputTopic, outputTopic)
        val processingStream = {

          inputTopic.subscribe(1000)
            .evalMap {
              case msg: Disconnect =>
                games
                  .values
                  .toList
                  .traverse(_.handle(msg))
                  .map(_.flatten)

              case msg: LobbyMessage =>
                games
                  .get(msg.game)
                  .map(_.handle(msg))
                  .getOrElse(Nil.pure[IO])

              case msg =>
                commonProcessor.handle(msg).pure[IO]
            }
            .flatMap(Stream.emits)
            .through(outputTopic.publish)
        }

        Stream(httpStream, processingStream)
          .parJoinUnbounded
          .compile
          .drain
          .as(ExitCode.Success)

      }} yield exitCode
  }
}

object Games {
  def init[F[_] : Async](implicit L: Logger[F]): Map[String, GameMessageHandler[F, Game]] = {
    Map(
      CardGame.name -> GameMessageHandler[F, CardGame](CardGame.lobbySize),
      DiceGame.name -> GameMessageHandler[F, DiceGame](CardGame.lobbySize)
    )
  }
}

object ServerStream {
  def stream[F[_] : Async](
    inputTopic: Topic[F, InputMessage],
    outputTopic: Topic[F, OutputMessage]
  ) (implicit config: Config, L: Logger[F]): Stream[F, ExitCode] = {

    BlazeServerBuilder[F]
      .bindHttp(config.server.port, config.server.host)
      .withHttpWebSocketApp(new ServerRoutes[F](inputTopic, outputTopic).wsRoutes(_).orNotFound)
      .serve
  }
}

