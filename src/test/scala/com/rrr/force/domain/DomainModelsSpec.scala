// src/test/scala/com/rrr/force/domain/DomainModelsSpec.scala
package com.rrr.force.domain

import java.time.Instant
import org.scalatest.funsuite.AnyFunSuite

class DomainModelsSpec extends AnyFunSuite {

  // 1. QueryAST & Filters
  test("Filter.Eq and Range should construct and match correctly") {
    val now = Instant.now()
    val eqFilter = Filter.Eq("eventType", "PushEvent")
    val rangeFilter = Filter.Range("createdAt", now.minusSeconds(3600), now)

    assert(eqFilter.field == "eventType")
    assert(eqFilter.value == "PushEvent")

    assert(rangeFilter.field == "createdAt")
    assert(rangeFilter.start.isBefore(rangeFilter.end))
  }

  test("QueryAST default values and custom instantiation") {
    val defaultAst = QueryAST()
    assert(defaultAst.filters.isEmpty)
    assert(defaultAst.groupBy.isEmpty)
    assert(defaultAst.aggregations.isEmpty)

    val ast = QueryAST(
      filters = Seq(Filter.Eq("actor", "pqr")),
      groupBy = Seq(GroupByKey.ByUser, GroupByKey.ByEventType),
      aggregations = Seq("count" -> AggOp.CountOp)
    )
    assert(ast.filters.head.isInstanceOf[Filter.Eq])
    assert(ast.groupBy.contains(GroupByKey.ByUser))
    assert(ast.aggregations.head._2 == AggOp.CountOp)
  }

  // 2. LogicalPlan nodes
  test("LogicalPlan FilteredPlan, JoinedPlan, AggregatedPlan, RootPlan") {
    val filtered = LogicalPlan.FilteredPlan("GitHubEvents", Seq(Filter.Eq("type", "WatchEvent")))
    val joined = LogicalPlan.JoinedPlan(filtered, "BroadcastData", Seq("org" -> "org"))
    val aggregated = LogicalPlan.AggregatedPlan(joined, Seq(GroupByKey.ByOrg), Seq("count" -> AggOp.CountOp))
    val root = LogicalPlan.RootPlan(aggregated)

    // verify hierarchy
    root.plan match {
      case LogicalPlan.AggregatedPlan(_, groupBy, aggs) =>
        assert(groupBy == Seq(GroupByKey.ByOrg))
        assert(aggs.head._2 == AggOp.CountOp)
      case _ => fail("Expected AggregatedPlan inside RootPlan")
    }
  }

  // 3. SubqueryPlan
  test("SubqueryPlan ties a LogicalPlan to partitionId") {
    val plan = LogicalPlan.FilteredPlan("GitHubEvents", Seq.empty)
    val sub = SubqueryPlan(plan, partitionId = 5)
    assert(sub.partitionId == 5)
    assert(sub.plan == plan)
  }

  // 4. PartialResult and FinalResult
  test("PartialResult and FinalResult data integrity") {
    val row1 = Map("eventType" -> "PushEvent", "count" -> 10)
    val row2 = Map("eventType" -> "WatchEvent", "count" -> 5)

    val partial = PartialResult(2, Seq(row1, row2))
    assert(partial.partitionId == 2)
    assert(partial.data.size == 2)
    assert(partial.data.head("count") == 10)

    val finalRes = FinalResult(Seq(row1, row2))
    assert(finalRes.data.contains(row2))
  }
}
