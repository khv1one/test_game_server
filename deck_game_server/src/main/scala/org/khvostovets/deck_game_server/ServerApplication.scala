package org.khvostovets.deck_game_server

import cats.effect.kernel.Async
import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps, toTraverseOps}
import fs2.Stream
import fs2.concurrent.Topic
import org.http4s.blaze.server.BlazeServerBuilder
import org.khvostovets.config.ConfigHelpers.createConfig
import org.khvostovets.deck_game_server.config.Config
import org.khvostovets.deck_game_server.game.{CardGame, DiceGame, Game}
import org.khvostovets.deck_game_server.message.{GameMessage, InputMessage, OutputMessage, Ping}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}

import java.util.UUID
import scala.concurrent.duration.DurationInt

object ServerApplication extends IOApp{
  override def run(args: List[String]): IO[ExitCode] = {

    implicit val conf: Config = createConfig[Config]()
    implicit def logger[F]: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    for {
      inputTopic <- Topic[IO, InputMessage]
      outputTopic <- Topic[IO, OutputMessage]

      gamesProcessors <- Games.init[IO]
      commonProcessor <- Ref.of[IO, CommonProcessor](CommonProcessor(gamesProcessors.keys.map(_.toString())))

      exitCode <- {
        val httpStream = ServerStream.stream[IO](inputTopic, outputTopic)
        val ping = Stream.awakeEvery[IO](1.seconds).map(_ => Ping).through(outputTopic.publish)
        val processingStream = //TODO: topic for each processor
          inputTopic.subscribe(1000)
            .evalMap {
              case msg: GameMessage =>
                gamesProcessors
                  .get(UUID.fromString(msg.game))
                  .map(_.modify(_.process(msg)))
                  .getOrElse(Nil.pure[IO])

              case msg =>
                commonProcessor.modify(_.process(msg))
            }
            .flatMap(Stream.emits)
            .through(outputTopic.publish)

        Stream(httpStream, /*ping, */processingStream)
          .parJoinUnbounded
          .compile
          .drain
          .as(ExitCode.Success)

      }} yield exitCode
  }
}

object Games {
  def init[F[_] : Async]: F[Map[UUID, Ref[F, GameProcessor[Game]]]] = {
    Map(
      CardGame.uuid -> Ref.of[F, GameProcessor[Game]](GameProcessor[CardGame]()),
      DiceGame.uuid -> Ref.of[F, GameProcessor[Game]](GameProcessor[DiceGame]())
    )
      .toList
      .traverse { case (k, vf) => vf.map(v => k -> v) }
      .map(_.toMap)
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

