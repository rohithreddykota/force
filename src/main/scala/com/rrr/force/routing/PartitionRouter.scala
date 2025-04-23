package com.rrr.force.routing

import com.rrr.force.domain.SubqueryPlan
import com.rrr.force.domain.LogicalPlan
import com.rrr.force.domain.GitHubEvent

object PartitionRouter {

  def route(
             plan:       LogicalPlan,
             events:     Seq[GitHubEvent],
             numParts:   Int
           ): Seq[SubqueryPlan] = {
    require(numParts > 0, "numParts must be positive")
    events.zipWithIndex.map { case (evt, idx) =>
      val pid = idx % numParts
      SubqueryPlan(plan, evt, pid)
    }
  }
}
