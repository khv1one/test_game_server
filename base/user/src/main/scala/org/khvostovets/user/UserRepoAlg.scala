package org.khvostovets.user

import cats.effect.{Async, Ref}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId, toFlatMapOps, toFunctorOps}

trait UserRepoAlg[F[_]] {
  def getByName(name: String): F[Option[User]]
  def addUser(user: User): F[Boolean]
  def removeUser(user: User): F[Unit]
  def changeUserScores(user: User, scores: Int): F[Option[User]]
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

    override def changeUserScores(user: User, scores: Int): F[Option[User]] = {
      usersByName
        .modify { usersByName =>
          usersByName.get(user.name).fold {
            (usersByName, Option.empty[User])
          } { user =>
            val nUser = user.copy(tokens = user.tokens + scores)

            (usersByName + (nUser.name -> nUser), nUser.some)
          }
        }
    }
  }

  object InMemory {
    def apply[F[_] : Async](): F[InMemory[F]] = {
      Ref.of[F, Map[String, User]](Map.empty).map(new InMemory[F](_))
    }
  }
}
