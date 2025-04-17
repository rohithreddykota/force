// src/test/scala/com/rrr/force/actors/BroadcastManagerActorSpec.scala
package com.rrr.force.actors

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.scalatest.wordspec.AnyWordSpecLike
import com.rrr.force.actors.Messages._
import com.rrr.force.broadcast.BroadcastData

class BroadcastManagerActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "BroadcastManagerActor" must {
    "serve up-to-date BroadcastData" in {
      val probe = TestProbe[BroadcastResponse]()
      val bm = spawn(BroadcastManagerActor())
      bm ! BroadcastRequest(probe.ref)
      val resp = probe.receiveMessage()
      // By default, BroadcastData.load() must succeed
      resp.data.users shouldBe a [Seq[_]]
      resp.data.orgs shouldBe a [Seq[_]] // if orgs field named correctly
    }
  }
}
