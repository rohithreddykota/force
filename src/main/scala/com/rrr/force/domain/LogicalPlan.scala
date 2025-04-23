// src/main/scala/com/rrr/force/domain/LogicalPlan.scala
package com.rrr.force.domain

/**
 * Represents the logical plan for query execution in DistribuQuery.
 * The plan is derived from the QueryAST and consists of transformations
 * such as filtering, joining, and aggregation.
 */
sealed trait LogicalPlan

final case class FilterByType(eventType: String) extends LogicalPlan

final case class ProjectActor(login: String) extends LogicalPlan


object LogicalPlan {
  /**
   * A plan node that filters a source dataset using a sequence of filters.
   * @param source The named source (e.g., "GitHubEvents").
   * @param filters The filters to apply.
   */
  final case class FilteredPlan(
                                 source: String,
                                 filters: Seq[Filter]
                               ) extends LogicalPlan

  /**
   * A plan node that joins the left logical plan with broadcast data.
   * @param left The input logical plan.
   * @param broadcastName The name of the broadcast data source (e.g., "BroadcastData").
   * @param joinKeys Pairs of (leftField, rightField) to join on.
   */
  final case class JoinedPlan(
                               left: LogicalPlan,
                               broadcastName: String,
                               joinKeys: Seq[(String, String)]
                             ) extends LogicalPlan

  /**
   * A plan node that groups the input plan by given keys and applies aggregations.
   * @param source The input logical plan.
   * @param groupBy The grouping keys.
   * @param aggs Sequence of (field, aggregation operation) to compute.
   */
  final case class AggregatedPlan(
                                   source: LogicalPlan,
                                   groupBy: Seq[GroupByKey],
                                   aggs: Seq[(String, AggOp)]
                                 ) extends LogicalPlan

  /**
   * The root of the logical plan tree.
   * @param plan The top-level plan node.
   */
  final case class RootPlan(plan: LogicalPlan) extends LogicalPlan
}
