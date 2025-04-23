// src/main/scala/com/rrr/force/executor/LocalExecutor.scala
package com.rrr.force.executor

import com.rrr.force.aggregation.{AggregationOp, Instances}
import com.rrr.force.domain.LogicalPlan.{AggregatedPlan, FilteredPlan, JoinedPlan, RootPlan}
import com.rrr.force.domain._

/**
 * Pure functions to execute a SubqueryPlan for a single GitHubEvent.
 */
object LocalExecutor {


  def execute(plan: SubqueryPlan): PartialResult = {


    val maybeEvt: Option[GitHubEvent] = applyFilters(plan.evt, plan.plan)

    val rows: Seq[Map[String,Any]] =
      maybeEvt.toSeq.map(e => Map("event" -> e))


    val aggRows: Seq[Map[String,Any]] =
      applyAggregation(rows, plan.plan)


    PartialResult(plan.partitionId, aggRows)
  }

  private def applyFilters(evt: GitHubEvent, lp: LogicalPlan): Option[GitHubEvent] = lp match {
    case FilterByType(tpe) =>
      if (evt.eventType == tpe) Some(evt) else None

    case ProjectActor(login) =>
      if (evt.actor.login == login) Some(evt) else None


    case FilteredPlan(_, filters) =>
      if (filters.forall(f => applyFilter(evt, f))) Some(evt) else None

    case JoinedPlan(left, _, _)     => applyFilters(evt, left)
    case AggregatedPlan(src, _, _)  => applyFilters(evt, src)
    case RootPlan(inner)            => applyFilters(evt, inner)
    case _                          => Some(evt)
  }


  private def applyFilter(ev: GitHubEvent, f: Filter): Boolean = f match {
    case Filter.Eq(field, value) =>
      val v = field match {
        case "eventType" => ev.eventType
        case "actor"     => ev.actor.login
        case "repo"      => ev.repo.name
        case "id"        => ev.id
        case _           => ""
      }
      v == value

    case Filter.Range(field, start, end) =>
      if (field == "createdAt") {
        val ts = ev.createdAt
        !ts.isBefore(start) && !ts.isAfter(end)
      } else false


  }

  private def applyAggregation(
                                rows: Seq[Map[String, Any]],
                                lp:   LogicalPlan
                              ): Seq[Map[String, Any]] = lp match {

    case LogicalPlan.RootPlan(inner) =>
      applyAggregation(rows, inner)

    case LogicalPlan.AggregatedPlan(_, groupBy, aggs) =>
      rows
        .groupBy { row =>
          groupBy.map {
            case GroupByKey.ByEventType => row("event").asInstanceOf[GitHubEvent].eventType
            case GroupByKey.ByOrg       => row("event").asInstanceOf[GitHubEvent].repo.name
            case GroupByKey.ByUser      => row("event").asInstanceOf[GitHubEvent].actor.login
            case GroupByKey.ByRepo      => row("event").asInstanceOf[GitHubEvent].repo.name
          }
        }
        .flatMap { case (keyVals, groupRows) =>

          val aggResults: Map[String, Any] = aggs.map { case (alias, op) =>
            val (inst, inputs) = op match {
              case AggOp.SumOp   =>
                (Instances.SumOpLong.asInstanceOf[AggregationOp[Any]],
                  groupRows.map(_.apply("event").asInstanceOf[PushEvent].size))
              case AggOp.CountOp =>
                (Instances.CountOp.asInstanceOf[AggregationOp[Any]],
                  groupRows.map(_ => 1))
              case AggOp.AvgOp   =>
                (Instances.AvgOp.asInstanceOf[AggregationOp[Any]],
                  groupRows.map(_.apply("event").asInstanceOf[PushEvent].size))
              case AggOp.UniqueOp =>
                (Instances.UniqueOp.asInstanceOf[AggregationOp[Any]],
                  groupRows.map(_.apply("event").asInstanceOf[GitHubEvent]))
            }
            val acc0 = inst.zero
            val accN = inputs.foldLeft(acc0)(inst.accumulate)
            alias -> inst.finish(accN)
          }.toMap


          val keyMap = groupBy.zip(keyVals).map {
            case (GroupByKey.ByEventType, v) => "eventType" -> v
            case (GroupByKey.ByOrg, v)       => "org"       -> v
            case (GroupByKey.ByUser, v)      => "user"      -> v
            case (GroupByKey.ByRepo, v)      => "repo"      -> v
          }.toMap

          Seq(aggResults ++ keyMap)
        }
        .toSeq

    case _ =>

      rows
  }
}
