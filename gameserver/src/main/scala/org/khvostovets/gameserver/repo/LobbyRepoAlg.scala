package org.khvostovets.gameserver.repo

import cats.effect.{Async, Ref}
import cats.implicits.toFunctorOps

trait LobbyRepoAlg[F[_], T] {
  def append(user: T) : F[Unit]
  def catOff(count: Int): F[List[T]]
  def size(): F[Int]
}

object LobbyRepoAlg {
  case class InMemory[F[_] : Async, T](users: Ref[F, List[T]]) extends LobbyRepoAlg[F, T] {

    override def append(user: T): F[Unit] =
      users.update(_ :+ user)

    override def catOff(count: Int): F[List[T]] = users.modify { users =>
      val (head, tail) = users.splitAt(count)

      if (head.size == count) {
        (tail, head)
      } else {
        (users, List.empty)
      }
    }

    override def size(): F[Int] = users.get.map(_.size)
  }

  object InMemory {
    def apply[F[_] : Async, T]() = new InMemory[F, T](Ref.unsafe[F, List[T]](List.empty))
  }
}