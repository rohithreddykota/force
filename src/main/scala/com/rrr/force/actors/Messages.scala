package com.rrr.force.actors

import com.rrr.force.models.{BroadcastData, GitHubEvent, Query}

/** Coordinator Actor Messages */
object CoordinatorMessage {
  case class QueryRequest(query: Query, userRole: String)

  case class QueryResponse(results: Seq[GitHubEvent])
}

/** Partition Manager Actor Messages */
object PartitionManagerMessage {
  case class GetPartitions(query: Query)

  case class Partitions(partitionIds: Seq[Int])
}

/** Broadcast Manager Actor Messages */
object BroadcastManagerMessage {
  case object RequestSmallDataset

  case class BroadcastSmallDataset(data: BroadcastData)
}

/** Worker Actor Messages */
object WorkerMessage {
  case class ProcessQuery(query: Query, broadcastData: Option[BroadcastData])

  case class QueryResult(results: Seq[GitHubEvent])
}
