// src/main/scala/com/rrr/force/actors/WorkerActor.scala
package com.rrr.force.actors

import akka.actor.typed.{Behavior, ActorRef}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.rrr.force.actors.Messages._
import com.rrr.force.executor.LocalExecutor
import com.rrr.force.monitoring.Monitoring
import com.rrr.force.storage.DataPartition

class WorkerActor(
                   ctx: ActorContext[ExecuteSubquery],
                   mon: Monitoring,
                   partition: DataPartition
                 ) extends AbstractBehavior[ExecuteSubquery](ctx) {
  override def onMessage(msg: ExecuteSubquery): Behavior[ExecuteSubquery] = {
    mon.logMetric("worker.received", 1)
    try {
      val pr = LocalExecutor.execute(
        msg.plan, msg.broadcast, Map(partition.id -> partition)
      )
      mon.logMetric("worker.completed", 1)
      msg.replyTo ! SubqueryResult(pr)
    } catch {
      case ex: Exception =>
        mon.logMetric("worker.error", 1)
        throw ex
    }
    this
  }
}

object WorkerActor {
  def apply(
             partition: DataPartition,
             mon: Monitoring
           ): Behavior[ExecuteSubquery] =
    Behaviors.setup(ctx => new WorkerActor(ctx, mon, partition))
}
