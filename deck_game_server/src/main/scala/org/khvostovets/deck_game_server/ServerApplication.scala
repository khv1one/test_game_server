package org.khvostovets.deck_game_server

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder
import org.khvostovets.config.ConfigHelpers.createConfig
import org.khvostovets.deck_game_server.config.Config
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object ServerApplication extends IOApp{
  override def run(args: List[String]): IO[ExitCode] = {

    implicit val conf: Config = createConfig[Config]()
    implicit def unsafeLogger[F]: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    val router = new ServerRoutes[IO]()

    BlazeServerBuilder[IO]
      .bindHttp(conf.server.port, conf.server.host)
      .withHttpWebSocketApp(router.wsRoutes(_).orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
