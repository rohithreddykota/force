// src/test/scala/com/rrr/force/planner/QueryPlannerSpec.scala
package com.rrr.force.planner

import com.rrr.force.domain._
import org.scalatest.funsuite.AnyFunSuite
import java.time.Instant

class QueryPlannerSpec extends AnyFunSuite {
  val now = Instant.parse("2025-01-01T00:00:00Z")

  test("plan should produce FilteredPlan -> AggregatedPlan without join when no org/user filter") {
    val ast = QueryAST(
      filters = Seq(Filter.Eq("eventType", "PushEvent")),
      groupBy = Seq(GroupByKey.ByEventType),
      aggregations = Seq("size" -> AggOp.SumOp)
    )

    val plan = QueryPlanner.plan(ast)
    plan match {
      case LogicalPlan.RootPlan(
      LogicalPlan.AggregatedPlan(
      LogicalPlan.FilteredPlan(src, filters),
      groupBy,
      aggs
      )
      ) =>
        assert(src == "GitHubEvents")
        assert(filters == ast.filters)
        assert(groupBy == ast.groupBy)
        assert(aggs == ast.aggregations)
      case other => fail(s"Unexpected plan structure: $other")
    }
  }

  test("plan should insert JoinedPlan when org filter present") {
    val ast = QueryAST(
      filters = Seq(Filter.Eq("org", "abc")),
      groupBy = Seq(GroupByKey.ByOrg),
      aggregations = Seq("count" -> AggOp.CountOp)
    )

    val plan = QueryPlanner.plan(ast)
    plan match {
      case LogicalPlan.RootPlan(
      LogicalPlan.AggregatedPlan(
      LogicalPlan.JoinedPlan(
      LogicalPlan.FilteredPlan(_, f2),
      bcName,
      joinKeys
      ),
      groupBy,
      aggs
      )
      ) =>
        assert(f2 == ast.filters)
        assert(bcName == "BroadcastData")
        assert(joinKeys.contains(("actor","login")))
        assert(joinKeys.contains(("repo","name")))
        assert(groupBy == ast.groupBy)
        assert(aggs == ast.aggregations)
      case other => fail(s"Unexpected plan structure: $other")
    }
  }

  test("plan should handle multiple filters and grouping without join if no org/user") {
    val ast = QueryAST(
      filters = Seq(
        Filter.Eq("eventType", "IssuesEvent"),
        Filter.Range("createdAt", now.minusSeconds(3600), now)
      ),
      groupBy = Seq(GroupByKey.ByUser, GroupByKey.ByRepo),
      aggregations = Seq("count" -> AggOp.CountOp, "uniqueUsers" -> AggOp.UniqueOp)
    )

    val plan = QueryPlanner.plan(ast)
    plan match {
      case LogicalPlan.RootPlan(
      LogicalPlan.AggregatedPlan(
      LogicalPlan.FilteredPlan(_, fList),
      groupByList,
      aggsList
      )
      ) =>
        assert(fList == ast.filters)
        assert(groupByList == ast.groupBy)
        assert(aggsList == ast.aggregations)
      case other => fail(s"Unexpected plan structure: $other")
    }
  }
}
