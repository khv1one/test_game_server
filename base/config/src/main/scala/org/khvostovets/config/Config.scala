package org.khvostovets.config

import scala.reflect.ClassTag

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.DerivedConfigReader
import pureconfig.generic.semiauto.deriveReader
import shapeless.Lazy

trait WithConfigCodec[T] {
  implicit def codec(implicit reader: Lazy[DerivedConfigReader[T]]): ConfigReader[T] = deriveReader[T]
}

object ConfigHelpers {
  def createConfig[CR: ConfigReader : ClassTag](): CR = ConfigSource.default.loadOrThrow[CR]
}
