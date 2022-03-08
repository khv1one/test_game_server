import Dependencies._

ThisBuild / scalaVersion := "2.13.8"

lazy val config = project.in(file("base/config"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(options)
  .settings(
    name := "Config",
    version := "0.1",
    libraryDependencies ++= Seq(
      logback,
      redisson,
      cats,
      catsEffect,
      circeGeneric,
      pureConfig,
      circeGenericExtras,
      pureConfig,
      fs2,
      logCatsSlf4,
      http4sServer,
      http4sEndpointsServer,
      //http4sDsl
    ),
    Universal / packageName := "config"
  )

lazy val user = project.in(file("base/user"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(options)
  .settings(
    name := "User",
    version := "0.1",
    Universal / packageName := "user"
  )

lazy val deck_game_server = project.in(file("deck_game_server"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(options)
  .settings(
    name := "Deck Game Server",
    version := "0.1",
    libraryDependencies ++= Seq(),
    Universal / packageName := "deck_game_server",
  )
  .dependsOn(config, user)

val options = scalacOptions ++= Seq(
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:existentials",
  "-language:postfixOps",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-Yrangepos",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-encoding", "utf8",
  "-language:higherKinds",
  //"-Xfatal-warnings"
)