// src/main/scala/com/rrr/force/actors/PartitionManagerActor.scala
package com.rrr.force.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.rrr.force.actors.Messages._
import com.rrr.force.utils.DefaultConfigParser

object PartitionManagerActor {
  def apply(): Behavior[PartitionRequest] = Behaviors.setup { ctx =>
    val cfg   = DefaultConfigParser.config
    val parts = cfg.getIntList("force.partitions").toArray.toSeq.collect {
      case i: java.lang.Integer => i.toInt
    }

    Behaviors.receiveMessage {
      case PartitionRequest(replyTo) =>
        ctx.log.info(s"PartitionManagerActor → $parts")
        replyTo ! PartitionResponse(parts)
        Behaviors.same
    }
  }
}
