package org.khvostovets.gameserver.repo

import cats.Parallel
import cats.effect.{Async, Ref}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxTuple2Parallel, toFlatMapOps, toFoldableOps, toFunctorOps}
import org.khvostovets.gameserver.system.GameSession

import java.util.UUID

trait SessionRepoAlg[F[_], T] {
  def add(session: GameSession[F, T]): F[Unit]
  def removeBySessionId(sessionId: UUID): F[Unit]
  def getBySessionId(sessionId: UUID): F[Option[GameSession[F, T]]]
  def getByUserId(id: String): F[Set[GameSession[F, T]]]
}

object SessionRepoAlg {
  case class InMemory[F[_] : Async, T](
    sessionByIdRef: Ref[F, Map[UUID, GameSession[F, T]]],
    sessionsByUserIdRef: Ref[F, Map[String, Set[GameSession[F, T]]]]
  ) extends SessionRepoAlg[F, T] {

    override def add(session: GameSession[F, T]): F[Unit] = {
      sessionByIdRef
        .update(_ + (session.uuid -> session))
        .flatMap { _ =>
          session.users.toList.traverse_ { user =>
            sessionsByUserIdRef.update { sessionsByUserId =>
              val userSessions = sessionsByUserId.getOrElse(user, Set.empty)

              sessionsByUserId + (user -> (userSessions + session))
            }
          }
        }
    }

    override def removeBySessionId(id: UUID): F[Unit] = {
      sessionByIdRef
        .getAndUpdate(_ - id)
        .flatMap { sessionsById =>
          sessionsById
            .get(id)
            .fold(().pure[F]) { sessionToDelete =>
              sessionToDelete.users.traverse_ { user =>
                sessionsByUserIdRef.update { sessionsByUserId =>
                  val sessions = sessionsByUserId.getOrElse(user, Set.empty) - sessionToDelete

                  if (sessions.isEmpty) {
                    sessionsByUserId - user
                  } else {
                    sessionsByUserId + (user -> sessions)
                  }
                }
              }
            }
        }
    }

    override def getBySessionId(id: UUID): F[Option[GameSession[F, T]]] = sessionByIdRef.get.map(_.get(id))

    override def getByUserId(id: String): F[Set[GameSession[F, T]]] = {
      sessionsByUserIdRef.get.map(_.getOrElse(id, Set.empty))
    }
  }

  object InMemory {
    def apply[F[_] : Async : Parallel, T](): F[InMemory[F, T]] = {
      (
        Ref.of[F, Map[UUID, GameSession[F, T]]](Map.empty),
        Ref.of[F, Map[String, Set[GameSession[F, T]]]](Map.empty)
      ).parTupled.map { case (sessionByIdRef, sessionsByUserIdRef) =>
        new InMemory[F, T](sessionByIdRef, sessionsByUserIdRef)
      }
    }
  }
}