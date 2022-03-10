package org.khvostovets.user

import cats.effect.{Async, Ref}
import cats.implicits.toFunctorOps

import java.util.UUID

trait UserRepoAlg[F[_]] {
  def getByUUID(uuid: UUID): F[Option[User]]
  def getByName(name: String): F[Option[User]]
  def addUser(user: User): F[Unit]
  def removeUser(user: User): F[Unit]
}

object UserRepoAlg {
  case class InMemory[F[_] : Async](
    users: Ref[F, Set[User]]
  ) extends UserRepoAlg[F] {

    override def getByUUID(uuid: UUID): F[Option[User]] = {
      users.get.map(_.find(_.id == uuid))
    }

    override def getByName(name: String): F[Option[User]] = {
      users.get.map(_.find(_.name == name))
    }

    override def addUser(user: User): F[Unit] = users.update(_ + user)

    override def removeUser(user: User): F[Unit] = users.update(_ - user)
  }

  object InMemory {
    def apply[F[_] : Async]() = new InMemory[F](Ref.unsafe[F, Set[User]](Set.empty[User]))
  }
}

