// src/test/scala/com/rrr/force/actors/PartitionManagerActorSpec.scala
package com.rrr.force.actors


import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import com.rrr.force.actors.Messages._
import org.scalatest.wordspec.AnyWordSpecLike

class PartitionManagerActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "PartitionManagerActor" must {
    "respond with configured partitions" in {
      val probe = TestProbe[PartitionResponse]()
      val pm = spawn(PartitionManagerActor())
      pm ! PartitionRequest(probe.ref)
      val resp = probe.receiveMessage()
      // Assuming application.conf has force.partitions = [0,1,2]
      resp.partitions should contain allElementsOf Seq(0, 1, 2)
    }
  }
}
