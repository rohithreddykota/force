// src/main/scala/com/rrr/force/domain/Events.scala
package com.rrr.force.domain

import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Base trait for all GitHub events in DistribuQuery.
 * Concrete event types extend this and share common fields.
 */
sealed trait GitHubEvent {
  def id: String

  def eventType: String

  def actor: User

  def repo: Repository

  def createdAt: Instant
}

object GitHubEvent {
  private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

  /**
   * Parse an ISO‐8601 timestamp (e.g. "2025-01-01T15:00:00Z") into Instant.
   */
  def parseInstant(ts: String): Instant =
    Instant.from(isoFormatter.parse(ts))
}


/**
 * Represents a GitHub user in events and broadcast datasets.
 *
 * @param id           Unique user ID
 * @param login        User login (key for joins)
 * @param displayLogin Optional display username
 * @param avatarUrl    URL to the user's avatar image
 */
case class User(
                 id: Long,
                 login: String,
                 displayLogin: Option[String],
                 avatarUrl: String
               )

/**
 * Represents a GitHub repository referenced by events.
 *
 * @param id   Unique repository ID
 * @param name Name of the repository
 */
case class Repository(
                       id: Long,
                       name: String
                     )

/**
 * WatchEvent (when a user stars/un-stars a repository).
 *
 * @param id        Event ID
 * @param actor     User who performed the watch
 * @param repo      Repository being watched
 * @param action    Action taken (e.g. "started")
 * @param createdAt Event timestamp
 */
case class WatchEvent(
                       id: String,
                       actor: User,
                       repo: Repository,
                       action: String,
                       createdAt: Instant
                     ) extends GitHubEvent {
  override val eventType: String = "WatchEvent"
}

final case class MemberEvent(
                              id:         String,
                              actor:      User,
                              repo:       Repository,
                              member:     User,
                              action:     String,
                              createdAt:  Instant,
                              org:        Organization
                            ) extends GitHubEvent {
  override def eventType: String = "MemberEvent"
}


/**
 * CreateEvent (when a branch, tag, or repo is created).
 *
 * @param id           Event ID
 * @param actor        User who created
 * @param repo         Repository context
 * @param ref          Optional name of branch or tag
 * @param refType      Type of ref ("branch"|"tag"|"repository")
 * @param masterBranch Optional master branch name
 * @param description  Optional description text
 * @param createdAt    Event timestamp
 */
case class CreateEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        ref: Option[String],
                        refType: String,
                        masterBranch: Option[String],
                        description: Option[String],
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "CreateEvent"
}

/**
 * Commit details for PushEvent.
 *
 * @param sha         Commit SHA
 * @param authorName  Author's name
 * @param authorEmail Author's email
 * @param message     Commit message
 * @param url         Commit URL
 */
case class Commit(
                   sha: String,
                   authorName: String,
                   authorEmail: String,
                   message: String,
                   url: String
                 )

/**
 * PushEvent (one or more commits pushed to a branch).
 *
 * @param id           Event ID
 * @param actor        User who pushed
 * @param repo         Repository where push occurred
 * @param size         Number of commits in this push
 * @param distinctSize Number of distinct commits
 * @param ref          Git ref (e.g. "refs/heads/main")
 * @param head         SHA of head commit
 * @param before       SHA before push
 * @param commits      Sequence of commit details
 * @param createdAt    Event timestamp
 */
case class PushEvent(
                      id: String,
                      actor: User,
                      repo: Repository,
                      size: Int,
                      distinctSize: Int,
                      ref: String,
                      head: String,
                      before: String,
                      commits: Seq[Commit],
                      createdAt: Instant
                    ) extends GitHubEvent {
  override val eventType: String = "PushEvent"
}



/**
 * Detailed information about an issue for IssuesEvent.
 *
 * @param number    Issue number
 * @param title     Issue title
 * @param state     Issue state ("open"|"closed" etc.)
 * @param createdAt Issue creation timestamp
 * @param updatedAt Issue last update timestamp
 */
case class IssueDetails(
                         number: Long,
                         title: String,
                         state: String,
                         createdAt: Instant,
                         updatedAt: Instant
                       )

/**
 * IssuesEvent (when an issue is opened, closed, commented, etc.).
 *
 * @param id        Event ID
 * @param actor     User who triggered the issue action
 * @param repo      Repository containing the issue
 * @param action    Action taken ("opened", "closed" etc.)
 * @param issue     Issue details
 * @param createdAt Event timestamp
 */
case class IssuesEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        action: String,
                        issue: IssueDetails,
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "IssuesEvent"

}


