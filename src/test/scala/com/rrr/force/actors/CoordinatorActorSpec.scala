// src/test/scala/com/rrr/force/actors/CoordinatorActorSpec.scala
package com.rrr.force.actors

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.scalatest.wordspec.AnyWordSpecLike
import com.rrr.force.actors.Messages._
import com.rrr.force.security.DefaultACLService
import com.rrr.force.monitoring.ConsoleMonitoring
import com.rrr.force.storage.DataPartition
import com.rrr.force.broadcast.BroadcastData
import com.rrr.force.domain._
import java.time.Instant

class CoordinatorActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "CoordinatorActor" must {
    "process a simple query end-to-end" in {
      // Setup dependencies
      val pmProbe = TestProbe[PartitionRequest]()
      val bmProbe = TestProbe[BroadcastRequest]()
      val partial = PartialResult(0, Seq(Map("count"->1)))
      val workerProbe = TestProbe[ExecuteSubquery]()
      // Coordinator under test
      val coordinator = spawn(CoordinatorActor(
        pm = pmProbe.ref,
        bm = bmProbe.ref,
        workerRouter = workerProbe.ref.narrow[ExecuteSubquery],
        acl = DefaultACLService,
        mon = ConsoleMonitoring
      ))
      // TestProbe for client reply
      val clientProbe = TestProbe[QueryResponse]()

      // 1) Send query
      val json = """{"filters":[{"type":"Eq","field":"eventType","value":"PushEvent"}],"groupBy":["ByEventType"],"aggregations":[{"field":"count","op":"CountOp"}]}"""
      coordinator ! QueryRequest(json, clientProbe.ref)

      // 2) PartitionManager ask
      val prReq = pmProbe.expectMessageType[PartitionRequest]
      prReq.replyTo ! PartitionResponse(Seq(0))

      // 3) BroadcastManager ask
      val brReq = bmProbe.expectMessageType[BroadcastRequest]
      brReq.replyTo ! BroadcastResponse(BroadcastData.empty)

      // 4) Worker router receives ExecuteSubquery
      val exec = workerProbe.expectMessageType[ExecuteSubquery]
      // Fake worker reply
      exec.replyTo ! SubqueryResult(partial)

      // 5) Client should get final success
      clientProbe.expectMessage(QueryResponse.Success(FinalResult(Seq(Map("count"->1)))))
    }
  }
}
