package com.rrr.force.utils

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Central loader for application.conf.
 * Use DefaultConfigParser.config to access settings.
 */
trait ConfigParser {
  def config: Config
}

object DefaultConfigParser extends ConfigParser {
  override val config: Config = ConfigFactory.load()
}
