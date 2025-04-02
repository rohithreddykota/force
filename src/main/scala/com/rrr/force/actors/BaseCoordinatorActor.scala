package com.rrr.force.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.rrr.force.models.{GitHubEvent, Query, BroadcastData}
import com.rrr.force.actors.CoordinatorMessage._
import com.rrr.force.security.DefaultACLService

/**
 * BaseCoordinatorActor defines the common interface for the Coordinator actor.
 * It is responsible for validating queries, retrieving partitions and broadcast data,
 * distributing subqueries to worker actors, and aggregating the results.
 */
abstract class BaseCoordinatorActor(partitionManager: ActorRef, broadcastManager: ActorRef)
  extends Actor with ActorLogging {

  /** Validate that the user role is authorized to execute the query. */
  def validateQuery(userRole: String, action: String = "execute_query"): Boolean =
    DefaultACLService.isAuthorized(userRole, action)

  /** Retrieve partition IDs from the Partition Manager for a given query. */
  def fetchPartitions(query: Query): Unit

  /** Retrieve small datasets (users, organizations) from the Broadcast Manager. */
  def fetchBroadcastData(): Unit

  /** Distribute the query and broadcast data to the appropriate worker actors. */
  def distributeSubqueries(query: Query, broadcastData: Option[BroadcastData]): Unit

  /** Aggregate the partial results from workers into a final query result. */
  def aggregateResults(): Seq[GitHubEvent]

  /** Abstract receive method; concrete implementations must define message handling. */
  override def receive: Receive
}

object CoordinatorActor {
  def props(partitionManager: ActorRef, broadcastManager: ActorRef): Props =
    Props.empty // Concrete implementation will override this.
}
