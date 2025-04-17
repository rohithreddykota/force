// src/main/scala/com/rrr/force/executor/LocalExecutor.scala
package com.rrr.force.executor

import com.rrr.force.aggregation.{AggregationOp, Instances}
import com.rrr.force.broadcast.BroadcastData
import com.rrr.force.domain._
import com.rrr.force.storage.DataPartition

/**
 * Pure functions to execute subquery plans locally.
 */
object LocalExecutor {

  /**
   * Execute a SubqueryPlan against in-memory partitions and broadcast data.
   */
  def execute(
               plan: SubqueryPlan,
               bc: BroadcastData,
               parts: Map[Int, DataPartition]
             ): PartialResult = {
    val partition = parts.getOrElse(plan.partitionId,
      throw new IllegalArgumentException(s"Unknown partition: ${plan.partitionId}"))

    val filtered = applyFilters(partition.events, plan.plan)
    val joined = applyJoin(filtered, plan.plan, bc)
    val aggregated = applyAggregation(joined, plan.plan)

    PartialResult(plan.partitionId, aggregated)
  }

  private def applyFilters(events: Seq[GitHubEvent], lp: LogicalPlan): Seq[GitHubEvent] = lp match {
    case LogicalPlan.FilteredPlan(_, filters) => events.filter(e => filters.forall(f => applyFilter(e, f)))
    case LogicalPlan.JoinedPlan(inner, _, _) => applyFilters(events, inner)
    case LogicalPlan.AggregatedPlan(inner, _, _) => applyFilters(events, inner)
    case LogicalPlan.RootPlan(inner) => applyFilters(events, inner)
  }

  private def applyFilter(ev: GitHubEvent, f: Filter): Boolean = f match {
    case Filter.Eq(field, value) =>
      val v = field match {
        case "eventType" => ev.eventType
        case "actor" => ev.actor.login
        case "repo" => ev.repo.name
        case "id" => ev.id
        case _ => ""
      }
      v == value
    case Filter.Range(field, start, end) =>
      field match {
        case "createdAt" =>
          val ts = ev.createdAt
          !ts.isBefore(start) && !ts.isAfter(end)
        case _ => false
      }
  }

  private def applyJoin(
                         events: Seq[GitHubEvent],
                         lp: LogicalPlan,
                         bc: BroadcastData
                       ): Seq[Map[String, Any]] = lp match {
    case LogicalPlan.JoinedPlan(inner, _, keys) =>
      applyFilters(events, inner).flatMap { ev =>
        bc.users.filter(u => keys.forall {
          case ("actor", "login") => u.login == ev.actor.login
          case _ => false
        }).map { u =>
          Map(
            "event" -> ev,
            "userName" -> u.name.getOrElse(u.login)
          )
        }
      }
    case _ => events.map(e => Map("event" -> e))
  }

  private def applyAggregation(
                                rows: Seq[Map[String, Any]],
                                lp: LogicalPlan
                              ): Seq[Map[String, Any]] = lp match {
    case LogicalPlan.RootPlan(inner) =>
      applyAggregation(rows, inner)

    case LogicalPlan.AggregatedPlan(_, groupBy, aggs) =>
      rows.groupBy { row =>
        groupBy.map {
          case GroupByKey.ByEventType => row("event").asInstanceOf[GitHubEvent].eventType
          case GroupByKey.ByOrg => row("event").asInstanceOf[GitHubEvent].repo.name
          case GroupByKey.ByUser => row("event").asInstanceOf[GitHubEvent].actor.login
          case GroupByKey.ByRepo => row("event").asInstanceOf[GitHubEvent].repo.name
        }
      }.map { case (keyVals, groupRows) =>
        val aggResults: Map[String, Any] = aggs.map {
          case (alias, op) =>
            val (instance, inputs) = op match {
              case AggOp.SumOp =>
                (
                  Instances.SumOpLong.asInstanceOf[AggregationOp[Any]],
                  groupRows.map(r => r("event").asInstanceOf[PushEvent].size)
                )
              case AggOp.CountOp =>
                (
                  Instances.CountOp.asInstanceOf[AggregationOp[Any]],
                  groupRows.map(_ => 1)
                )
              case AggOp.AvgOp =>
                (
                  Instances.AvgOp.asInstanceOf[AggregationOp[Any]],
                  groupRows.map(r => r("event").asInstanceOf[PushEvent].size)
                )
              case AggOp.UniqueOp =>
                (
                  Instances.UniqueOp.asInstanceOf[AggregationOp[Any]],
                  groupRows.map(r => r("event").asInstanceOf[GitHubEvent])
                )
            }
            val acc0 = instance.zero
            val accN = inputs.foldLeft(acc0)(instance.accumulate)
            alias -> instance.finish(accN)
        }.toMap
        val keyMap = groupBy.zip(keyVals).map {
          case (GroupByKey.ByEventType, v) => "eventType" -> v
          case (GroupByKey.ByOrg, v) => "org" -> v
          case (GroupByKey.ByUser, v) => "user" -> v
          case (GroupByKey.ByRepo, v) => "repo" -> v
        }.toMap
        aggResults ++ keyMap
      }.toSeq

    case _ => rows
  }
}