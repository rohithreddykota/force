// src/main/scala/com/rrr/force/domain/SubqueryPlan.scala
package com.rrr.force.domain

/**
 * A SubqueryPlan ties a LogicalPlan to a specific partition ID.
 * Worker actors execute these plans on their local partition.
 *
 * @param plan        The logical plan fragment to execute.
 * @param partitionId The partition identifier.
 */
final case class SubqueryPlan(
                               plan: LogicalPlan,
                               partitionId: Int
                             )
