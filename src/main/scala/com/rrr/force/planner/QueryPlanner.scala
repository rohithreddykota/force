// src/main/scala/com/rrr/force/planner/QueryPlanner.scala
package com.rrr.force.planner

import com.rrr.force.domain._

/**
 * QueryPlanner constructs a LogicalPlan from a validated QueryAST.
 * It applies a sequence of transformations: filter, optional join, and aggregation.
 */
object QueryPlanner {

  /**
   * Build a LogicalPlan from the AST:
   * 1. Filter the "GitHubEvents" source by AST.filters
   * 2. If any filter on "org" or "user", join with "BroadcastData"
   * 3. Aggregate by AST.groupBy and AST.aggregations
   * 4. Wrap in RootPlan
   *
   * @param ast Validated QueryAST
   * @return RootPlan containing the full plan tree
   */
  def plan(ast: QueryAST): LogicalPlan = {
    // 1. filtering
    val filtered: LogicalPlan = LogicalPlan.FilteredPlan(
      source = "GitHubEvents",
      filters = ast.filters
    )

    // 2. optional join with broadcast data (users/orgs)
    val joined: LogicalPlan = {
      // if any Eq filter on actor or repo owner, require broadcast
      val needsJoin = ast.filters.exists {
        case Filter.Eq(field, _) if field.equalsIgnoreCase("org")    => true
        case Filter.Eq(field, _) if field.equalsIgnoreCase("user")   => true
        case _                                                          => false
      }
      if (needsJoin) {
        LogicalPlan.JoinedPlan(
          left = filtered,
          broadcastName = "BroadcastData",
          joinKeys = Seq(
            // left field -> right field mapping
            ("actor", "login"),
            ("repo", "name")
          )
        )
      } else filtered
    }

    // 3. aggregation
    val aggregated: LogicalPlan = LogicalPlan.AggregatedPlan(
      source = joined,
      groupBy = ast.groupBy,
      aggs    = ast.aggregations
    )

    // 4. wrap
    LogicalPlan.RootPlan(aggregated)
  }
}
