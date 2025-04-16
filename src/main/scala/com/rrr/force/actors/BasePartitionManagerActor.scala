package com.rrr.force.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rrr.force.actors.PartitionManagerMessage._
import com.rrr.force.models.Query

/**
 * BasePartitionManagerActor defines the interface for partition management.
 * It determines the relevant dataset partitions for a given query.
 */
abstract class BasePartitionManagerActor extends Actor with ActorLogging {

  /** Determine and return a sequence of partition IDs based on the query. */
  def determinePartitions(query: Query): Seq[Int]

  override def receive: Receive = {
    case GetPartitions(query) =>
      val partitions = determinePartitions(query)
      sender() ! Partitions(partitions)
  }
}

object PartitionManagerActor {
  def props(): Props = Props.empty // Concrete implementation will be provided.
}
