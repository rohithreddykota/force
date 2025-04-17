package com.rrr.force.monitoring

trait Monitoring {
  /** log a gauge / counter */
  def logMetric(name: String, value: Long): Unit
}

/** Print to console or integrate with Prometheus/Grafana */
object ConsoleMonitoring extends Monitoring {
  override def logMetric(name: String, value: Long): Unit =
    println(s"[METRIC] $name -> $value")
}
