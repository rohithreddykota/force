// src/main/scala/com/rrr/force/actors/CoordinatorActor.scala
package com.rrr.force.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{Success, Failure}

import com.rrr.force.actors.Messages._
import com.rrr.force.parser.QueryParser
import com.rrr.force.planner.QueryPlanner
import com.rrr.force.security.ACLService
import com.rrr.force.domain.{LogicalPlan, PartialResult, SubqueryPlan}
import com.rrr.force.aggregator.ResultAggregator
import com.rrr.force.monitoring.Monitoring

object CoordinatorActor {

  /**
   * @param workerCnt      Balance 里配置的分区/子流数量
   * @param workerRouter   pool router，用来下发 ExecuteSubquery
   * @param acl            ACLService，用来鉴权
   * @param mon            Monitoring
   */
  def apply(
             workerCnt:    Int,
             workerRouter: ActorRef[ExecuteSubquery],
             acl:          ACLService,
             mon:          Monitoring): Behavior[Any] =
    Behaviors.setup { ctx =>
      implicit val timeout: Timeout = 5.seconds
      implicit val ec              = ctx.executionContext

      var replyToCli : Option[ActorRef[QueryResponse]] = None
      var partials    = Vector.empty[PartialResult]

      Behaviors.receiveMessage {

        // ---------------- CLI 触发一次查询 ----------------
        case QueryRequest(json, replyTo) =>
          replyToCli = Some(replyTo)
          partials   = Vector.empty
          mon.logMetric("coord.request", 1)

          QueryParser.parseQuery(json) match {
            case Left(err) =>
              replyTo ! QueryResponse.Failure(s"parse error: $err")

            case Right(ast) =>
              acl.authorize(ast) match {
                case Left(reason) =>
                  replyTo ! QueryResponse.Failure(s"unauthorized: $reason")

                case Right(_) =>
                  // ① 生成逻辑计划
                  val lp: LogicalPlan = QueryPlanner.plan(ast)
                  // ② 按 [0 .. workerCnt‑1] 直接构造 SubqueryPlan
                  (0 until workerCnt).foreach { pid =>
                    val sp = SubqueryPlan(lp, null, pid)
                    workerRouter ! ExecuteSubquery(sp, ctx.self)
                  }
              }
          }
          Behaviors.same

        // --------------- Worker 返回部分结果 ---------------
        case SubqueryResult(pr) =>
          mon.logMetric("coord.partial", 1)
          partials :+= pr
          if (partials.size >= workerCnt) {
            val res = ResultAggregator.merge(partials)
            mon.logMetric("coord.completed", 1)
            replyToCli.foreach(_ ! QueryResponse.Success(res))
            partials = Vector.empty
          }
          Behaviors.same

        case other =>
          ctx.log.warn(s"[Coordinator] unexpected $other")
          Behaviors.unhandled
      }
    }
}
