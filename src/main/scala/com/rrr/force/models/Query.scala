package com.rrr.force.models

import java.time.Instant

case class Query(
                  dateRange: Option[(Instant, Instant)] = None,
                  eventType: Option[String] = None,
                  repository: Option[String] = None,
                  userRole: String = "default"
                )

case class QueryResult(events: Seq[GitHubEvent])
