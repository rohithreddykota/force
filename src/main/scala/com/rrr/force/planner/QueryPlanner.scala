package com.rrr.force.planner

import com.rrr.force.domain._
import com.rrr.force.utils.PlannerConfig

/**
 * QueryPlanner constructs a LogicalPlan from a validated QueryAST,
 * reading source/broadcast names and join-key mappings from config.
 */
object QueryPlanner {
  // Load once from application.conf
  private val plannerConfig = PlannerConfig()

  def plan(ast: QueryAST): LogicalPlan = {
    // 1. filtering
    val filtered: LogicalPlan = LogicalPlan.FilteredPlan(
      source = plannerConfig.dataSourceName,
      filters = ast.filters
    )

    // 2. optional join
    val needsJoin = ast.filters.exists {
      case Filter.Eq(field, _) if field.equalsIgnoreCase("org") => true
      case Filter.Eq(field, _) if field.equalsIgnoreCase("user") => true
      case _ => false
    }

    val joined: LogicalPlan =
      if (needsJoin) LogicalPlan.JoinedPlan(
        left = filtered,
        broadcastName = plannerConfig.broadcastDataName,
        joinKeys = plannerConfig.joinKeys
      )
      else filtered

    // 3. aggregation
    val aggregated: LogicalPlan = LogicalPlan.AggregatedPlan(
      source = joined,
      groupBy = ast.groupBy,
      aggs = ast.aggregations
    )

    // 4. wrap
    LogicalPlan.RootPlan(aggregated)
  }
}
