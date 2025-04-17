// src/test/scala/com/rrr/force/executor/LocalExecutorSpec.scala
package com.rrr.force.executor

import org.scalatest.funsuite.AnyFunSuite
import com.rrr.force.domain._
import com.rrr.force.storage.DataPartition
import com.rrr.force.broadcast.BroadcastData
import com.rrr.force.executor.LocalExecutor
import java.time.Instant

class LocalExecutorSpec extends AnyFunSuite {
  // Helper to create a PushEvent
  private def mkPush(id: String, actorLogin: String, size: Int, ts: String): PushEvent = {
    val user = User(1, actorLogin, None, "")
    val repo = Repository(1, "repo")
    val commit = Commit("sha", "n", "e", "msg", "url")
    PushEvent(id, user, repo, size, size, "ref", "head", "before", Seq(commit), Instant.parse(ts))
  }

  test("execute filter + aggregate count of PushEvent") {
    val now = "2025-01-01T00:00:00Z"
    val ev1 = mkPush("1", "u1", 1, now)
    val ev2 = mkPush("2", "u2", 2, now)
    val ev3 = mkPush("3", "u1", 3, now)
    // one WatchEvent should be filtered out
    val watch = WatchEvent("4", User(2, "u1", None, ""), Repository(1, "repo"), "started", Instant.parse(now))

    val partition = DataPartition(0, Seq(ev1, ev2, ev3, watch))
    val parts = Map(0 -> partition)
    val bc = BroadcastData(Seq.empty, Seq.empty)

    // LogicalPlan: Filter eventType == PushEvent, groupBy ByEventType, count
    val lp = LogicalPlan.RootPlan(
      LogicalPlan.AggregatedPlan(
        LogicalPlan.FilteredPlan("src", Seq(Filter.Eq("eventType", "PushEvent"))),
        Seq(GroupByKey.ByEventType),
        Seq("count" -> AggOp.CountOp)
      )
    )
    val plan = SubqueryPlan(lp, 0)

    val pr = LocalExecutor.execute(plan, bc, parts)
    assert(pr.partitionId == 0)
    // Expect count = 3
    val row = pr.data.head
    assert(row("count") == 3)
    assert(row("eventType") == "PushEvent")
  }
}
