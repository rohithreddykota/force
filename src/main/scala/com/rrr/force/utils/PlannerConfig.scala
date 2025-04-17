// src/main/scala/com/rrr/force/utils/PlannerConfig.scala
package com.rrr.force.utils

import com.typesafe.config.Config
import scala.collection.JavaConverters._

/**
 * Configuration for the QueryPlanner.
 *
 * @param dataSourceName     Name of the main data source (e.g., "GitHubEvents").
 * @param broadcastDataName  Name of the broadcast table (e.g., "BroadcastData").
 * @param joinKeys           Sequence of (leftField, rightField) pairs for joins.
 */
case class PlannerConfig(
                          dataSourceName: String,
                          broadcastDataName: String,
                          joinKeys: Seq[(String, String)]
                        )

object PlannerConfig {
  // Single shared config instance
  private val cfg: Config = DefaultConfigParser.config

  /** Load PlannerConfig from the default application.conf. */
  def apply(): PlannerConfig = fromConfig(cfg)

  /** Load PlannerConfig from an explicit Config. */
  def fromConfig(config: Config): PlannerConfig = {
    // Ensure required keys exist
    Seq(
      "force.data-source-name",
      "force.broadcast-data-name",
      "force.join-keys"
    ).foreach { path =>
      if (!config.hasPath(path))
        throw new IllegalStateException(s"Missing configuration key: $path")
    }

    val dsName = config.getString("force.data-source-name")
    val bdName = config.getString("force.broadcast-data-name")

    // Parse join-keys list of objects
    val jk = config.getConfigList("force.join-keys").asScala.toSeq.map { c =>
      (c.getString("left-field"), c.getString("right-field"))
    }

    PlannerConfig(dsName, bdName, jk)
  }
}
