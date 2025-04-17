package com.rrr.force.utils

import com.typesafe.config.Config

/**
 * Reads out planner settings from application.conf:
 * - dataSourceName
 * - broadcastDataName
 * - joinKeys: Seq[(leftField, rightField)]
 */
case class PlannerConfig(
                          dataSourceName: String,
                          broadcastDataName: String,
                          joinKeys: Seq[(String, String)]
                        )

object PlannerConfig {
  private val cfg = DefaultConfigParser.config

  import scala.collection.JavaConverters._

  /**
   * Load PlannerConfig from default application.conf
   */
  def apply(): PlannerConfig = fromConfig(cfg)

  /**
   * Load PlannerConfig from provided Config
   */
  def fromConfig(config: Config): PlannerConfig = {
    val dsName = config.getString("force.data-source-name")
    val bdName = config.getString("force.broadcast-data-name")
    val jk = config.getConfigList("force.join-keys").asScala.toSeq.map { c =>
      (c.getString("left-field"), c.getString("right-field"))
    }
    PlannerConfig(dsName, bdName, jk)
  }
}
