package distribuquery.monitoring

/**
 * Trait for monitoring and logging metrics.
 *
 * This trait defines the contract for reporting various metrics, such as:
 * - Mailbox sizes
 * - Processing times
 * - Actor restarts, etc.
 *
 * It supports both basic metric logging and logging with additional context (tags).
 */
trait Monitoring {

  /**
   * Logs a metric with the given name and value.
   *
   * @param metricName The name of the metric.
   * @param value      The value of the metric.
   */
  def logMetric(metricName: String, value: Double): Unit

  /**
   * Logs a metric with the given name, value, and additional tags.
   *
   * @param metricName The name of the metric.
   * @param value      The value of the metric.
   * @param tags       A map of additional context tags.
   */
  def logMetric(metricName: String, value: Double, tags: Map[String, String]): Unit = {
    // Default implementation ignores tags; override if needed.
    logMetric(metricName, value)
  }
}

/**
 * Default implementation of the Monitoring trait.
 *
 * This implementation simply prints metrics to the console.
 * In a production system, you would integrate with a proper metrics library (e.g., Prometheus, Datadog).
 */
object DefaultMonitoring extends Monitoring {
  override def logMetric(metricName: String, value: Double): Unit = {
    println(s"[METRIC] $metricName = $value")
  }
}
