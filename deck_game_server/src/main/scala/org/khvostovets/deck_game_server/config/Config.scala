package org.khvostovets.deck_game_server.config

import org.khvostovets.config.ConfigCodec

case class ServerConfig(port: Int, host: String)

case class Config(server: ServerConfig)

object ServerConfig extends ConfigCodec[ServerConfig]

object Config extends ConfigCodec[Config]