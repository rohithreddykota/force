package com.rrr.force.actors

import akka.actor.Props
import com.rrr.force.models._
import com.rrr.force.query.DefaultQueryProcessor

class WorkerActor(partitionId: Int, partitionData: Seq[GitHubEvent]) extends BaseWorkerActor(partitionId) {

    override def processSubquery(query: Query, broadcastData: Option[BroadcastData]): Seq[GitHubEvent] = {
        val queryProcessor = DefaultQueryProcessor
        queryProcessor.process(query, partitionData, broadcastData)
    }
}

object WorkerActor {
    def props(partitionId: Int, partitionData: Seq[GitHubEvent]): Props =
        Props(new WorkerActor(partitionId, partitionData))
}
