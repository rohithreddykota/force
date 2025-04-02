package com.rrr.force.models

import java.time.Instant

// ---------------------------------------
// GitHub Event Domain Models
// ---------------------------------------

/**
 * Base trait for all GitHub events.
 * All event types (Push, Pull Request, etc.) extend this trait.
 */
sealed trait GitHubEvent {
  def id: String

  def eventType: String

  def actor: User

  def repository: Repository

  def createdAt: Instant
}

/**
 * Represents a GitHub Push event.
 */
case class PushEvent(
                      id: String,
                      actor: User,
                      repository: Repository,
                      payload: PushPayload,
                      createdAt: Instant
                    ) extends GitHubEvent {
  override val eventType: String = "PushEvent"
}

/**
 * Represents a GitHub Pull Request event.
 */
case class PullRequestEvent(
                             id: String,
                             actor: User,
                             repository: Repository,
                             payload: PullRequestPayload,
                             createdAt: Instant
                           ) extends GitHubEvent {
  override val eventType: String = "PullRequestEvent"
}

// Additional GitHub event types (e.g., IssuesEvent, ForkEvent) can be defined similarly.

// ---------------------------------------
// Event Payloads
// ---------------------------------------

/**
 * Payload for a PushEvent.
 */
case class PushPayload(
                        ref: String,
                        before: String,
                        after: String,
                        commits: Seq[Commit]
                      )

/**
 * Payload for a PullRequestEvent.
 */
case class PullRequestPayload(
                               action: String,
                               number: Int,
                               pullRequest: PullRequest
                             )

// ---------------------------------------
// Supporting Domain Models
// ---------------------------------------

/**
 * Represents a commit associated with a PushEvent.
 */
case class Commit(
                   sha: String,
                   message: String,
                   url: String
                 )

/**
 * Represents a Pull Request.
 */
case class PullRequest(
                        id: Long,
                        title: String,
                        body: Option[String],
                        merged: Boolean
                      )

/**
 * Represents a GitHub user.
 */
case class User(
                 id: Long,
                 login: String,
                 name: Option[String],
                 email: Option[String]
               )

/**
 * Represents a GitHub organization.
 */
case class Organization(
                         id: Long,
                         login: String,
                         name: Option[String]
                       )

/**
 * Represents a GitHub repository.
 */
case class Repository(
                       id: Long,
                       name: String,
                       owner: User
                     )

// ---------------------------------------
// Query & Broadcast Data Models
// ---------------------------------------

/**
 * Represents a query filter submitted by a Data Analyst.
 * Supports filtering by date range, event type, actor, repository, and additional criteria.
 */
case class Query(
                  dateRange: Option[(Instant, Instant)] = None,
                  eventType: Option[String] = None,
                  actor: Option[String] = None,
                  repository: Option[String] = None,
                  additionalFilters: Map[String, String] = Map.empty
                )

/**
 * Holds small datasets (e.g., users and organizations) used for join operations
 * with the main event data.
 */
case class BroadcastData(
                          users: Seq[User],
                          organizations: Seq[Organization]
                        )
