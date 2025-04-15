package com.rrr.force.models

import java.time.Instant

// GitHub event
sealed trait GitHubEvent {
  def id: String
  def eventType: String
  def actor: User
  def repo: Repository
  def createdAt: Instant
}

case class User(
                 id: Long,
                 login: String,
                 display_login: String,
                 gravatar_id: String,
                 url: Option[String],
                 avatar_url: String
               )

case class Repository(id: Long, name: String, url: String)

case class Organization(
                         id: Integer,
                         login: String,
                         gravatar_id: String,
                         url: String,
                         avatar_url: String
                       )

case class BroadcastData(
                          users: Seq[User],
                          organizations: Seq[Organization]
                        )

// --- CreateEvent and Payload ---
case class CreateEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        payload: CreatePayload,
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "CreateEvent"
}

case class CreatePayload(
                          ref: String,
                          ref_type: String,
                          master_branch: String,
                          description: String,
                          pusher_type: String
                        )

// --- DeleteEvent and Payload ---
case class DeleteEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        payload: DeletePayload,
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "DeleteEvent"
}

case class DeletePayload(
                          ref: String,
                          ref_type: String
                        )

// --- ForkEvent and Payload ---
case class ForkEvent(
                      id: String,
                      actor: User,
                      repo: Repository,
                      payload: ForkPayload,
                      createdAt: Instant
                    ) extends GitHubEvent {
  override val eventType: String = "ForkEvent"
}

case class ForkPayload(
                        forkee: Repository
                      )

// --- PushEvent and Payload ---
case class PushEvent(
                      id: String,
                      actor: User,
                      repo: Repository,
                      payload: PushPayload,
                      createdAt: Instant
                    ) extends GitHubEvent {
  override val eventType: String = "PushEvent"
}

case class PushPayload(
                        ref: String,
                        before: String,
                        after: String,
                        commits: Seq[Commit]
                      )

case class Commit(
                   sha: String,
                   message: String,
                   url: String
                 )

// --- PullRequestEvent and Payload ---
case class PullRequestEvent(
                             id: String,
                             actor: User,
                             repo: Repository,
                             payload: PullRequestPayload,
                             createdAt: Instant
                           ) extends GitHubEvent {
  override val eventType: String = "PullRequestEvent"
}

case class PullRequestPayload(
                               action: String,
                               number: Int,
                               pullRequest: PullRequest
                             )

case class PullRequest(
                        id: Long,
                        title: String,
                        body: Option[String],
                        state: String,
                        merged: Boolean
                      )

// --- IssuesEvent and Payload ---
case class IssuesEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        payload: IssuesPayload,
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "IssuesEvent"
}

case class IssuesPayload(
                          action: String,
                          issue: Issue
                        )

case class Issue(
                  id: Long,
                  title: String,
                  body: Option[String],
                  state: String,
                  labels: Seq[String]
                )

// --- IssueCommentEvent and Payload ---
case class IssueCommentEvent(
                              id: String,
                              actor: User,
                              repo: Repository,
                              payload: IssueCommentPayload,
                              createdAt: Instant
                            ) extends GitHubEvent {
  override val eventType: String = "IssueCommentEvent"
}

case class IssueCommentPayload(
                                action: String,
                                issue: Issue,
                                comment: Comment
                              )

case class Comment(
                    id: Long,
                    body: String,
                    user: User
                  )

// --- PullRequestReviewEvent and Payload ---
case class PullRequestReviewEvent(
                                   id: String,
                                   actor: User,
                                   repo: Repository,
                                   payload: PullRequestReviewPayload,
                                   createdAt: Instant
                                 ) extends GitHubEvent {
  override val eventType: String = "PullRequestReviewEvent"
}

case class PullRequestReviewPayload(
                                     action: String,
                                     review: Review
                                   )

case class Review(
                   id: Long,
                   body: String,
                   state: String
                 )

// --- PullRequestReviewCommentEvent and Payload ---
case class PullRequestReviewCommentEvent(
                                          id: String,
                                          actor: User,
                                          repo: Repository,
                                          payload: PullRequestReviewCommentPayload,
                                          createdAt: Instant
                                        ) extends GitHubEvent {
  override val eventType: String = "PullRequestReviewCommentEvent"
}

case class PullRequestReviewCommentPayload(
                                            action: String,
                                            comment: Comment,
                                            pullRequest: PullRequest
                                          )

// --- MemberEvent and Payload ---
case class MemberEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        payload: MemberPayload,
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "MemberEvent"
}

case class MemberPayload(
                          action: String,
                          member: User
                        )

// --- PublicEvent ---
case class PublicEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "PublicEvent"
}

// --- ReleaseEvent and Payload ---
case class ReleaseEvent(
                         id: String,
                         actor: User,
                         repo: Repository,
                         payload: ReleasePayload,
                         createdAt: Instant
                       ) extends GitHubEvent {
  override val eventType: String = "ReleaseEvent"
}

case class ReleasePayload(
                           action: String,
                           release: Release
                         )

case class Release(
                    id: Long,
                    tag_name: String,
                    name: Option[String],
                    body: Option[String],
                    draft: Boolean,
                    prerelease: Boolean
                  )

// --- WatchEvent ---
case class WatchEvent(
                       id: String,
                       actor: User,
                       repo: Repository,
                       createdAt: Instant
                     ) extends GitHubEvent {
  override val eventType: String = "WatchEvent"
}

// --- GollumEvent and Payload ---
case class GollumEvent(
                        id: String,
                        actor: User,
                        repo: Repository,
                        payload: GollumPayload,
                        createdAt: Instant
                      ) extends GitHubEvent {
  override val eventType: String = "GollumEvent"
}

case class GollumPayload(
                          pages: Seq[GollumPage]
                        )

case class GollumPage(
                       page_name: String,
                       title: String,
                       summary: Option[String],
                       action: String,
                       sha: String,
                       html_url: String
                     )
