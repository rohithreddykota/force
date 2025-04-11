package com.rrr.force.query

import com.rrr.force.models._

object DefaultQueryProcessor extends QueryProcessor {

    override def process(query: Query, events: Seq[GitHubEvent], broadcastData: Option[BroadcastData]): Seq[GitHubEvent] = {
        val filtered = events
            .filter(eventMatchesQuery(_, query))

        broadcastData match {
            case Some(data) => enrichEvents(filtered, data)
            case None       => filtered
        }
    }

    private def eventMatchesQuery(event: GitHubEvent, query: Query): Boolean = {
        val withinDateRange = query.dateRange.forall {
            case (start, end) =>
                !event.createdAt.isBefore(start) && !event.createdAt.isAfter(end)
        }

        val eventTypeMatches = query.eventType.forall(_ == event.eventType)

        val actorMatches = query.actor.forall(_ == event.actor.login)

        val repoMatches = query.repository.forall(_ == event.repository.name)

        val additionalMatches = query.additionalFilters.forall {
            case ("commitMessageContains", value) =>
                event match {
                    case pe: PushEvent =>
                        pe.payload.commits.exists(_.message.contains(value))
                    case _ => false
                }
            case _ => true // ignore unknown filters
        }

        withinDateRange && eventTypeMatches && actorMatches && repoMatches && additionalMatches
    }

    private def enrichEvents(events: Seq[GitHubEvent], broadcastData: BroadcastData): Seq[GitHubEvent] = {
        val userMap = broadcastData.users.map(u => u.login -> u).toMap
        if (userMap.isEmpty) return events // skip enrichment

        events.map {
            case pe: PushEvent =>
                val enrichedActor = userMap.getOrElse(pe.actor.login, pe.actor)
                pe.copy(actor = enrichedActor)

            case pre: PullRequestEvent =>
                val enrichedActor = userMap.getOrElse(pre.actor.login, pre.actor)
                pre.copy(actor = enrichedActor)

            case other => other
        }
    }
}
