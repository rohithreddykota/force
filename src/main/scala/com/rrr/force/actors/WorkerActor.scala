package com.rrr.force.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.rrr.force.actors.Messages.{ExecuteSubquery, SubqueryResult}
import com.rrr.force.executor.LocalExecutor
import com.rrr.force.monitoring.Monitoring

/**
 * Stateless worker: executes a single SubqueryPlan and replies with PartialResult.
 */
object WorkerActor {
  def apply(mon: Monitoring): Behavior[ExecuteSubquery] =
    Behaviors.receive { (ctx, msg) =>
      mon.logMetric("worker.received", 1)
      try {
        val pr = LocalExecutor.execute(msg.plan)
        mon.logMetric("worker.completed", 1)
        msg.replyTo ! SubqueryResult(pr)
      } catch {
        case ex: Exception =>
          mon.logMetric("worker.error", 1)
          throw ex
      }
      Behaviors.same
    }
}