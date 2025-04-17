package com.rrr.force.utils

import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
 * Configuration for QueryPlanner.
 * - force.data-source-name
 * - force.broadcast-data-name
 * - force.join-keys = [ { left-field, right-field }, … ]
 */
case class PlannerConfig(
                          dataSourceName: String,
                          broadcastDataName: String,
                          joinKeys: Seq[(String, String)]
                        )

object PlannerConfig {
  private val cfg: Config = DefaultConfigParser.config

  def apply(): PlannerConfig = fromConfig(cfg)

  def fromConfig(config: Config): PlannerConfig = {
    // validate
    Seq("force.data-source-name", "force.broadcast-data-name", "force.join-keys").foreach { path =>
      if (!config.hasPath(path))
        throw new IllegalStateException(s"Missing config key: $path")
    }
    val ds = config.getString("force.data-source-name")
    val bd = config.getString("force.broadcast-data-name")
    val jk = config.getConfigList("force.join-keys").asScala.toSeq.map { c =>
      (c.getString("left-field"), c.getString("right-field"))
    }
    PlannerConfig(ds, bd, jk)
  }
}
