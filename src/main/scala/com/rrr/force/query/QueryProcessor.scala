package com.rrr.force.query

import com.rrr.force.models.{BroadcastData, GitHubEvent, Query}

/**
 * Trait for query processing.
 *
 * This trait defines the functional logic required to process a query on a dataset of GitHub events.
 * Concrete implementations should provide:
 * - Filtering logic based on query criteria (e.g., date range, event type, actor, repository).
 * - Optional join/enrichment logic with broadcast data (such as users and organizations).
 *
 * @example
 * {{{
 *   object DefaultQueryProcessor extends QueryProcessor {
 *     def process(query: Query, events: Seq[GitHubEvent], broadcastData: Option[BroadcastData]): Seq[GitHubEvent] = {
 *       // Apply filtering based on query parameters
 *       val filtered = events.filter { event =>
 *         query.eventType.forall(_ == event.eventType) &&
 *         query.actor.forall(_ == event.actor.login) &&
 *         query.repository.forall(_ == event.repository.name) &&
 *         query.dateRange.forall { case (start, end) =>
 *           event.createdAt.isAfter(start) && event.createdAt.isBefore(end)
 *         }
 *       }
 *
 *       // Optionally perform enrichment join with broadcast data if provided
 *       broadcastData match {
 *         case Some(data) =>
 *           // Example: enrich event data with additional user/org info from broadcast data.
 *           enrichedEvents(filtered, data)
 *         case None =>
 *           filtered
 *       }
 *     }
 *
 *     private def enrichedEvents(events: Seq[GitHubEvent], broadcast: BroadcastData): Seq[GitHubEvent] = {
 *       // Implement join logic to enrich events using broadcast data.
 *       events // Placeholder: return events as-is.
 *     }
 *   }
 * }}}
 */
trait QueryProcessor {

  /**
   * Processes the provided query on the given sequence of GitHub events.
   *
   * @param query          The query filters (e.g., event type, date range, etc.).
   * @param events         The dataset of GitHub events to be processed.
   * @param broadcastData  Optional broadcast data (e.g., users, organizations) for join/enrichment.
   * @return               A sequence of GitHub events filtered and optionally enriched based on the query.
   */
  def process(query: Query, events: Seq[GitHubEvent], broadcastData: Option[BroadcastData]): Seq[GitHubEvent]
}
