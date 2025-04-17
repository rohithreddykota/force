// src/main/scala/com/rrr/force/actors/BaseActors.scala
package com.rrr.force.actors

import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.{Behaviors, ActorContext}
import com.rrr.force.monitoring.Monitoring

object BaseActors {
  /** Inject Monitoring into a behavior. */
  def withMonitoring[T](mkMon: ActorContext[T] => Monitoring)
                       (factory: (ActorContext[T], Monitoring) => Behavior[T]): Behavior[T] =
    Behaviors.setup { ctx =>
      val mon = mkMon(ctx)
      factory(ctx, mon)
    }

  /** Supervise children with restart on failure. */
  def restartOnFailure[T](behavior: Behavior[T]): Behavior[T] =
    Behaviors.supervise(behavior)
      .onFailure[Throwable](SupervisorStrategy.restart)
}
