// src/main/scala/com/rrr/force/routing/PartitionRouter.scala
package com.rrr.force.routing

import com.rrr.force.domain.{LogicalPlan, SubqueryPlan}

/**
 * PartitionRouter takes a LogicalPlan and a sequence of partition IDs,
 * and produces one SubqueryPlan per partition.
 *
 * This is a pure, functional module with no side effects.
 */
object PartitionRouter {

  /**
   * Routes the given logical plan to the specified partitions.
   *
   * @param plan       The logical plan to execute.
   * @param partitions A non-empty sequence of partition IDs.
   * @return A sequence of SubqueryPlan, one for each partition ID.
   * @throws IllegalArgumentException if partitions is empty.
   */
  def route(plan: LogicalPlan, partitions: Seq[Int]): Seq[SubqueryPlan] = {
    require(partitions.nonEmpty, "PartitionRouter.route: partitions must be non-empty")
    partitions.map(pid => SubqueryPlan(plan, pid))
  }
}
