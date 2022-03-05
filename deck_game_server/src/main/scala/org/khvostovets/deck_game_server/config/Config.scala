package org.khvostovets.deck_game_server.config

import org.khvostovets.config.WithConfigCodec

case class ServerConfig(port: Int, host: String)

case class Config(server: ServerConfig)

object ServerConfig extends WithConfigCodec[ServerConfig]

object Config extends WithConfigCodec[Config]