// src/main/scala/com/rrr/force/utils/MyPlanner.scala
package com.rrr.force.utils

import com.rrr.force.domain.{GitHubEvent, LogicalPlan, FilterByType, ProjectActor}

/**
 * Planner that transforms GitHubEvent instances into executable subquery plans.
 */
object MyPlanner {
  /**
   * Convert a GitHubEvent into a LogicalPlan for execution.
   * Extend the match cases to support additional event types or plan logic.
   */
  def plan(evt: GitHubEvent): LogicalPlan = evt.eventType match {
    case "PushEvent"    => FilterByType("PushEvent")
    case "WatchEvent"   => FilterByType("WatchEvent")
    case "CreateEvent"  => FilterByType("CreateEvent")
    case "IssuesEvent"  => FilterByType("IssuesEvent")
    case other           => ProjectActor(evt.actor.login)
  }
}
