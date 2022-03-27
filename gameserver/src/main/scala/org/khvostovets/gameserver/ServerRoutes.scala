package org.khvostovets.gameserver

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import fs2.concurrent.Topic
import fs2.{Pipe, Stream}
import io.circe.syntax.EncoderOps
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Text}
import org.http4s.{HttpRoutes, StaticFile}
import org.khvostovets.gameserver.message.{Disconnect, InputMessage, MessageParser, OutputMessage}
import org.khvostovets.user.{User, UserRepoAlg}
import org.typelevel.log4cats.Logger

class ServerRoutes[F[_] : Async](
  input: Topic[F, InputMessage],
  output: Topic[F, OutputMessage],
  userRepo: UserRepoAlg[F]
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

    case POST -> Root / "user" / name =>
      userRepo
        .getByName(name)
        .flatMap {
          _.fold {
            val user = User(name)

            userRepo.addUser(user).flatMap(_ => Ok(user.asJson))
          } { user =>
            Ok(user.asJson)
          }
        }

    case GET -> Root / "ws" / userName =>

      val outputPipe: Stream[F, WebSocketFrame] = {
        output
          .subscribe(1000)
          .filter(_.forUser(userName))
          .map(msg => Text(msg.toString))
      }

      def processInput(wsfStream: Stream[F, WebSocketFrame]): Stream[F, Unit] = {
        wsfStream
          .evalMap(msg => userRepo.getByName(userName).map { userO => (msg, userO) })
          .collect {
            case (Text(text, _), Some(user)) => MessageParser.parse(user, text)
            case (Close(_), Some(user)) => Disconnect(user)
          }
          .evalTap(msg => L.info(msg.toString))
          .through(a => input.publish(a))
      }

      val inputPipe: Pipe[F, WebSocketFrame, Unit] = processInput

      wsb.build(outputPipe, inputPipe)
  }
}