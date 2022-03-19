package org.khvostovets.gameserver

import cats.effect.Async
import fs2.concurrent.Topic
import fs2.{Pipe, Stream}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Text}
import org.http4s.{HttpRoutes, StaticFile}
import org.khvostovets.gameserver.message.{Disconnect, InputMessage, MessageParser, OutputMessage}
import org.typelevel.log4cats.Logger

class ServerRoutes[F[_] : Async](
  input: Topic[F, InputMessage],
  output: Topic[F, OutputMessage]
) (implicit L: Logger[F]) extends Http4sDsl[F] {

  def wsRoutes(wsb: WebSocketBuilder2[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ GET -> Root =>
      StaticFile
        .fromResource("static/index.html", Some(request))
        .getOrElseF(NotFound())

    case request @ GET -> Root / "chat.js" =>
      StaticFile
        .fromResource("static/chat.js", Some(request))
        .getOrElseF(NotFound())

    case GET -> Root / "ws" / user =>

      val outputPipe: Stream[F, WebSocketFrame] = {
        output
          .subscribe(1000)
          .filter(_.forUser(user))
          .map(msg => Text(msg.toString))
      }

      def processInput(wsfStream: Stream[F, WebSocketFrame]): Stream[F, Unit] = {
        wsfStream
          .collect {
            case Text(text, _) => MessageParser.parse(user, text)
            case Close(_) => Disconnect(user)
          }
          .evalTap(msg => L.info(s"message: $msg"))
          .through(input.publish)
      }

      val inputPipe: Pipe[F, WebSocketFrame, Unit] = processInput

      wsb.build(outputPipe, inputPipe)
  }
}