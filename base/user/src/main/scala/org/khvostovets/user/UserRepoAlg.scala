package org.khvostovets.user

import cats.effect.{Async, Ref}
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps}

trait UserRepoAlg[F[_]] {
  def getByName(name: String): F[Option[User]]
  def addUser(user: User): F[Boolean]
  def removeUser(user: User): F[Unit]
}

object UserRepoAlg {
  case class InMemory[F[_] : Async](
    usersByName: Ref[F, Map[String, User]]
  ) extends UserRepoAlg[F] {

    override def getByName(name: String): F[Option[User]] = usersByName.get.map(_.get(name))

    override def addUser(user: User): F[Boolean] = {
      getByName(user.name)
        .flatMap {
          _.fold(
            usersByName.update(_ + (user.name -> user)).map(_ => true)
          )(_ =>
            false.pure[F]
          )
        }
    }

    override def removeUser(user: User): F[Unit] = usersByName.update(_ - user.name)
  }

  object InMemory {
    def apply[F[_] : Async](): F[InMemory[F]] = {
      Ref.of[F, Map[String, User]](Map.empty).map(new InMemory[F](_))
    }
  }
}
