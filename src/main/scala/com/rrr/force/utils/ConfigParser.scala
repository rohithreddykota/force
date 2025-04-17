// src/main/scala/com/rrr/force/utils/ConfigParser.scala
package com.rrr.force.utils

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Central loader for application.conf.
 * All modules should use DefaultConfigParser.config to access settings.
 */
trait ConfigParser {
  def config: Config
}

object DefaultConfigParser extends ConfigParser {
  override val config: Config = ConfigFactory.load()
}
