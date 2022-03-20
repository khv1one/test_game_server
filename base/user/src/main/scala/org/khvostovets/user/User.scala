package org.khvostovets.user

import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

case class User(
  name: String,
  tokens: Int
)

trait UserJson {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val userEncoder: Encoder[User] = deriveConfiguredEncoder
}

object User extends UserJson {
  def apply(name: String): User = new User(name, 100)
}