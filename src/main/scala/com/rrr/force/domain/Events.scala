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
  private val isoFormatter = DateTimeFormatter.ISO_INSTANT

  /** Parse an ISO‐8601 timestamp (e.g. "2025-01-01T15:00:00Z") into Instant. */
  def parseInstant(ts: String): Instant =
    Instant.from(isoFormatter.parse(ts))
}

/** Supporting domain models shared across events */
case class User(
                 id: Long,
                 login: String,
                 displayLogin: Option[String],
                 avatarUrl: String
               )

case class Repository(
                       id: Long,
                       name: String
                     )

/** 1. WatchEvent (user stars or unstars a repo) */
case class WatchEvent(
                       id: String,
                       actor: User,
                       repo: Repository,
                       action: String, // e.g. "started"
                       createdAt: Instant
                     ) extends GitHubEvent {
  override val eventType: String = "WatchEvent"
}

/** 2. CreateEvent (branch, tag, or repository creation) */
case class CreateEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        ref: Option[String], // branch/tag name, None for repo creation
                        refType: String, // "branch" | "tag" | "repository"
                        masterBranch: Option[String],
                        description: Option[String],
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "CreateEvent"
}

/** Commit details for PushEvent */
case class Commit(
                   sha: String,
                   authorName: String,
                   authorEmail: String,
                   message: String,
                   url: String
                 )

/** 3. PushEvent (one or more commits pushed) */
case class PushEvent(
                      id: String,
                      actor: User,
                      repo: Repository,
                      size: Int,
                      distinctSize: Int,
                      ref: String, // e.g. "refs/heads/main"
                      head: String,
                      before: String,
                      commits: Seq[Commit],
                      createdAt: Instant
                    ) extends GitHubEvent {
  override val eventType: String = "PushEvent"
}

/** Issue details for IssuesEvent */
case class IssueDetails(
                         number: Long,
                         title: String,
                         state: String,
                         createdAt: Instant,
                         updatedAt: Instant
                       )

/** 4. IssuesEvent (when an issue is opened, closed, etc.) */
case class IssuesEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        action: String, // e.g. "opened"
                        issue: IssueDetails,
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "IssuesEvent"
}
