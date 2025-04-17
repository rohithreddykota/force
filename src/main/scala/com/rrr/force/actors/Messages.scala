// src/main/scala/com/rrr/force/actors/Messages.scala
package com.rrr.force.actors

import akka.actor.typed.ActorRef
import com.rrr.force.domain.{FinalResult, PartialResult, SubqueryPlan}
import com.rrr.force.broadcast.BroadcastData

object Messages {
  // Client → Coordinator
  final case class QueryRequest(json: String, replyTo: ActorRef[QueryResponse])
  sealed trait QueryResponse
  object QueryResponse {
    final case class Success(result: FinalResult) extends QueryResponse
    final case class Failure(reason: String)     extends QueryResponse
  }

  // Coordinator → PartitionManager
  final case class PartitionRequest(replyTo: ActorRef[PartitionResponse])
  final case class PartitionResponse(partitions: Seq[Int])

  // Coordinator → BroadcastManager
  final case class BroadcastRequest(replyTo: ActorRef[BroadcastResponse])
  final case class BroadcastResponse(data: BroadcastData)

  // Coordinator → Worker
  final case class ExecuteSubquery(
                                    plan: SubqueryPlan,
                                    broadcast: BroadcastData,
                                    replyTo: ActorRef[SubqueryResult]
                                  )
  final case class SubqueryResult(result: PartialResult)
}
