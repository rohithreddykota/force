package com.rrr.force.query

import com.rrr.force.domain.{QueryAST, SubqueryPlan, PartialResult, FinalResult}
import com.rrr.force.parser.QueryParser
import com.rrr.force.planner.QueryPlanner
import com.rrr.force.routing.PartitionRouter
import com.rrr.force.storage.DataPartition
import com.rrr.force.broadcast.BroadcastData
import com.rrr.force.executor.LocalExecutor
import com.rrr.force.aggregator.ResultAggregator

object QueryProcessor {
  def run(json: String,
          partitions: Seq[DataPartition],
          broadcast: BroadcastData): Either[String, FinalResult] =
    for {
      ast   <- QueryParser.parseQuery(json).left.map(_.toString)
      plan  =  QueryPlanner.plan(ast)
      subs  =  PartitionRouter.route(plan, partitions.map(_.id))
      partials = subs.map(sp => LocalExecutor.execute(sp, broadcast, partitions.map(p => p.id -> p).toMap))
    } yield ResultAggregator.merge(partials)
}
