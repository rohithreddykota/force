package com.rrr.force.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rrr.force.models.BroadcastData
import com.rrr.force.actors.BroadcastManagerMessage._

/**
 * BaseBroadcastManagerActor defines the interface for managing broadcast data.
 * It is responsible for retrieving and broadcasting small datasets to worker actors.
 */
abstract class BaseBroadcastManagerActor extends Actor with ActorLogging {

  /** Retrieve the small dataset to be broadcasted. */
  def getSmallDataset(): BroadcastData

  /** Optionally, proactively broadcast the small dataset to all workers. */
  def broadcastSmallDataset(): Unit

  override def receive: Receive = {
    case RequestSmallDataset =>
      sender() ! BroadcastSmallDataset(getSmallDataset())
    // Additional message handling (e.g., proactive broadcasting) can be added here.
  }
}

object BroadcastManagerActor {
  def props(): Props = Props.empty // Concrete implementation to be defined.
}
