// src/test/scala/com/rrr/force/monitoring/MonitoringSpec.scala
package com.rrr.force.monitoring

import org.scalatest.funsuite.AnyFunSuite

class MonitoringSpec extends AnyFunSuite {
  test("ConsoleMonitoring logs without error") {
    ConsoleMonitoring.logMetric("test.metric", 123)
    succeed
  }
}
