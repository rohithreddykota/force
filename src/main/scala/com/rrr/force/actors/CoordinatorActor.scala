// src/main/scala/com/rrr/force/actors/CoordinatorActor.scala
package com.rrr.force.actors

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import com.rrr.force.actors.Messages._
import com.rrr.force.parser.QueryParser
import com.rrr.force.security.ACLService
import com.rrr.force.planner.QueryPlanner
import com.rrr.force.routing.PartitionRouter
import com.rrr.force.aggregator.ResultAggregator
import com.rrr.force.domain.{LogicalPlan, PartialResult, QueryAST}
import com.rrr.force.broadcast.BroadcastData
import com.rrr.force.monitoring.Monitoring

object CoordinatorActor {

  /**
   * This actor receives:
   *   - QueryRequest(json, replyTo)
   *   - PartitionResponse(ids)
   *   - BroadcastResponse(data)
   *   - SubqueryResult(result)
   * all under a single Behavior[Any] so we can pipeToSelf easily.
   */
  def apply(
             pm: ActorRef[PartitionRequest],
             bm: ActorRef[BroadcastRequest],
             workerRouter: ActorRef[ExecuteSubquery],
             acl: ACLService,
             mon: Monitoring
           ): Behavior[Any] = Behaviors.setup { ctx =>
    implicit val scheduler = ctx.system.scheduler
    implicit val timeout: Timeout = 5.seconds
    implicit val ec = ctx.executionContext

    // Mutable holder for context across the pipeline
    var originalReply: Option[ActorRef[QueryResponse]] = None
    var originalAst:   Option[QueryAST]                = None
    var partitions:    Seq[Int]                         = Seq.empty
    var broadcastData: BroadcastData                    = BroadcastData.empty

    Behaviors.receiveMessage {
      // ------------------------------------------------------------------------
      // STEP 1: Client submits QueryRequest(json, replyTo)
      // ------------------------------------------------------------------------
      case QueryRequest(json, replyTo: ActorRef[QueryResponse]) =>
        QueryParser.parseQuery(json) match {
          case Left(err) =>
            // parse error
            replyTo ! QueryResponse.Failure(s"Parse error: $err")
          case Right(ast) =>
            // auth
            acl.authorize(ast) match {
              case Left(reason) =>
                replyTo ! QueryResponse.Failure(s"Unauthorized: $reason")
              case Right(_) =>
                // store AST & replyTo
                originalReply = Some(replyTo)
                originalAst   = Some(ast)
                // ask PartitionManager
                pm.ask(ref => PartitionRequest(ref)).onComplete {
                  case Success(pr) => ctx.self ! pr
                  case Failure(ex) => ctx.self ! PartitionResponse(Seq.empty)
                }
            }
        }
        Behaviors.same

      // ------------------------------------------------------------------------
      // STEP 2: Got PartitionResponse(ids)
      // ------------------------------------------------------------------------
      case PartitionResponse(ids) =>
        partitions = ids
        // ask BroadcastManager
        bm.ask(ref => BroadcastRequest(ref)).onComplete {
          case Success(br) => ctx.self ! br
          case Failure(_)  => ctx.self ! BroadcastResponse(BroadcastData.empty)
        }
        Behaviors.same

      // ------------------------------------------------------------------------
      // STEP 3: Got BroadcastResponse(data)
      // ------------------------------------------------------------------------
      case BroadcastResponse(data) =>
        broadcastData = data
        // plan & route
        val ast  = originalAst.get
        val plan = QueryPlanner.plan(ast)
        val subs = PartitionRouter.route(plan, partitions)
        // fan out to workers
        subs.foreach { sp =>
          workerRouter ! ExecuteSubquery(sp, broadcastData, ctx.self)
        }
        Behaviors.same

      // ------------------------------------------------------------------------
      // STEP 4: Collect SubqueryResult(partial)
      // ------------------------------------------------------------------------
      case SubqueryResult(pr: PartialResult) =>
        // For simplicity: merge immediately. In prod, accumulate all pr before merging.
        val finalRes = ResultAggregator.merge(Seq(pr))
        originalReply.foreach(_.tell(QueryResponse.Success(finalRes)))
        Behaviors.same

      // ------------------------------------------------------------------------
      // Unknown message
      // ------------------------------------------------------------------------
      case other =>
        ctx.log.warn(s"CoordinatorActor received unexpected message: $other")
        Behaviors.same
    }
  }
}
