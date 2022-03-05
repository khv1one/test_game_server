package org.khvostovets.deck_game_server

import cats.effect.Async
import fs2.{Pipe, Stream}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.{HttpRoutes, StaticFile}
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt

class ServerRoutes[F[_] : Async]()(implicit L: Logger[F]) extends Http4sDsl[F] {

  def wsRoutes(wsb: WebSocketBuilder2[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case request@GET -> Root =>
      StaticFile
        .fromResource("static/index.html", Some(request))
        .getOrElseF(
          NotFound()
        )

    case request@GET -> Root / "chat.js" =>
      StaticFile
        .fromResource("static/chat.js", Some(request))
        .getOrElseF(
          NotFound()
        )

    case GET -> Root / "ws" =>
      wsb.build(outputPipe, inputPipe)
  }

  val inputPipe: Pipe[F, WebSocketFrame, Unit] = _.evalMap {
    case Text(t, _) => L.info(t)
    case f => L.info(s"Unknown type: $f")
  }

  val outputPipe: Stream[F, WebSocketFrame] = Stream
    .awakeDelay[F](5.seconds)
    .map(_ => Text("echo"))
}