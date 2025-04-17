// src/main/scala/com/rrr/force/actors/BroadcastManagerActor.scala
package com.rrr.force.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.rrr.force.actors.Messages._
import com.rrr.force.broadcast.BroadcastData

/**
 * BroadcastManagerActor serves up-to-date BroadcastData on each request.
 */
object BroadcastManagerActor {

  def apply(): Behavior[BroadcastRequest] =
    Behaviors.receive { (ctx, msg) =>
      // Load fresh each time (or memoize externally if needed)
      val data = BroadcastData.load()
      msg.replyTo ! BroadcastResponse(data)
      Behaviors.same
    }
}
