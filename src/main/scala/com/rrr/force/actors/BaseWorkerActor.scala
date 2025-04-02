package com.rrr.force.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rrr.force.models.{GitHubEvent, Query, BroadcastData}
import com.rrr.force.actors.WorkerMessage._

/**
 * BaseWorkerActor defines the interface for worker actors.
 * Each worker processes a subquery on a local data partition and optionally joins with broadcast data.
 */
abstract class BaseWorkerActor(val partitionId: Int) extends Actor with ActorLogging {

  /** Process the subquery on the local partition with optional broadcast data. */
  def processSubquery(query: Query, broadcastData: Option[BroadcastData]): Seq[GitHubEvent]

  override def receive: Receive = {
    case ProcessQuery(query, broadcastData) =>
      val result = processSubquery(query, broadcastData)
      sender() ! QueryResult(result)
  }
}

object WorkerActor {
  def props(partitionId: Int): Props = Props.empty // Concrete implementation will be provided.
}
