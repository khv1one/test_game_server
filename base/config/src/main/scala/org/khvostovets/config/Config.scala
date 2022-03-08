package org.khvostovets.config

import scala.reflect.ClassTag

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.DerivedConfigReader
import pureconfig.generic.semiauto.deriveReader
import shapeless.Lazy

trait ConfigCodec[T] {
  implicit def codec(implicit reader: Lazy[DerivedConfigReader[T]]): ConfigReader[T] = deriveReader[T]
}

object ConfigHelpers {
  def createConfig[T: ConfigReader : ClassTag](): T = ConfigSource.default.loadOrThrow[T]
}
