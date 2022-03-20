import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.11"

  lazy val redisson = "org.redisson" % "redisson" % "3.16.8"

  lazy val endpoints = "org.endpoints4s" %% "algebra" % "1.7.0"
  lazy val endpointsCirce = "org.endpoints4s" %% "algebra-circe"  % "2.1.0"

  lazy val http4sServer = "org.http4s" %% "http4s-blaze-server" % "0.23.10"
  lazy val http4sClient = "org.http4s" %% "http4s-blaze-client" % "0.23.10"
  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % "0.23.10"

  lazy val http4sEndpointsServer = "org.endpoints4s" %% "http4s-server" % "9.0.0"
  lazy val http4sEndpointsClient = "org.endpoints4s"  %% "http4s-client" % "6.1.0"

  lazy val cats = "org.typelevel" %% "cats-core" % "2.7.0"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.5"

  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.17.1"

  lazy val fs2 = "co.fs2" %% "fs2-core" % "3.2.5"

  lazy val circe = "io.circe" %% "circe-core" % "0.14.1"
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.14.1"
  lazy val circeGenericExtras = "io.circe" %% "circe-generic-extras" % "0.14.1"
  lazy val circeParser = "io.circe" %% "circe-parser" % "0.14.1"
  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % "0.23.10"

  lazy val logCatsSlf4 = "org.typelevel" %% "log4cats-slf4j"   % "2.2.0"
}
