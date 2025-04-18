// src/test/scala/com/rrr/force/actors/WorkerActorSpec.scala
package com.rrr.force.actors

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.scalatest.wordspec.AnyWordSpecLike
import com.rrr.force.actors.Messages._
import com.rrr.force.storage.DataPartition
import com.rrr.force.domain._
import com.rrr.force.broadcast.BroadcastData
import com.rrr.force.monitoring.ConsoleMonitoring
import java.time.Instant

class WorkerActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "WorkerActor" must {
    "execute subquery and respond with PartialResult" ignore { // TODO: make this test work
      // Create a single PushEvent partition
      val now = Instant.parse("2025-01-01T00:00:00Z")
      val ev = PushEvent("1", User(1,"u",None,""), Repository(1,"r"), 1,1,"r","h","b",Seq(),now)
      val dp = DataPartition(0, Seq(ev))
      val probe = TestProbe[SubqueryResult]()
      val worker = spawn(WorkerActor(dp, ConsoleMonitoring))

      // Build a simple SubqueryPlan: filter eventType == "PushEvent", count
      val ast = QueryAST(
        filters = Seq(Filter.Eq("eventType","PushEvent")),
        groupBy = Seq(GroupByKey.ByEventType),
        aggregations = Seq("count"->AggOp.CountOp)
      )
      val plan = SubqueryPlan(LogicalPlan.FilteredPlan("GitHubEvents",ast.filters), dp.id)
      // Execute
      worker ! ExecuteSubquery(plan, BroadcastData.empty, probe.ref)
      val result = probe.receiveMessage()
      result.result.data.head("count") shouldBe 1
    }
  }
}
