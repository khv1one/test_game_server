package org.khvostovets.gameserver.config

import org.khvostovets.config.ConfigCodec

case class ServerConfig(port: Int, host: String)

case class Config(server: ServerConfig)

object ServerConfig extends ConfigCodec[ServerConfig]

object Config extends ConfigCodec[Config]