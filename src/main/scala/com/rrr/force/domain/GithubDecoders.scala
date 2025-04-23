// src/main/scala/com/rrr/force/domain/GitHubDecoders.scala
package com.rrr.force.domain

import cats.implicits.catsSyntaxApplicativeError
import io.circe.{Decoder, DecodingFailure, HCursor}
import io.circe.generic.semiauto._

import java.time.Instant

object GitHubDecoders {
  // Manual decoder for User to handle snake_case JSON fields
  implicit val userDecoder: Decoder[User] = Decoder.instance { c: HCursor =>
    for {
      idVal        <- c.get[Long]("id")
      login        <- c.get[String]("login")
      displayLogin <- c.get[Option[String]]("display_login").orElse(c.get[Option[String]]("displayLogin"))
      avatarUrl    <- c.get[Option[String]]("avatar_url").orElse(c.get[Option[String]]("avatarUrl")).flatMap {
        case Some(url) => Right(url)
        case None      => Left(DecodingFailure("Missing avatar_url/avatarUrl", c.history))
      }
    } yield User(idVal, login, displayLogin, avatarUrl)
  }

  // Decoder for Organization (snake_case fields)
  implicit val organizationDecoder: Decoder[Organization] = Decoder.instance { c: HCursor =>
    for {
      idVal  <- c.get[Long]("id")
      login  <- c.get[String]("login")
      avatar <- c.get[Option[String]]("avatar_url").orElse(c.get[Option[String]]("avatarUrl"))
    } yield Organization(idVal, login, avatar)
  }

  // Derive decoders for simple case classes
  implicit val repositoryDecoder:   Decoder[Repository]   = deriveDecoder
  implicit val issueDetailsDecoder: Decoder[IssueDetails] = deriveDecoder
  implicit val commitDecoder:       Decoder[Commit]       = deriveDecoder

  // Event decoders via deriveDecoder where structure matches
  implicit val pushEventDecoder:   Decoder[PushEvent]   = deriveDecoder
  implicit val watchEventDecoder:  Decoder[WatchEvent]  = deriveDecoder
  implicit val createEventDecoder: Decoder[CreateEvent] = deriveDecoder
  implicit val issuesEventDecoder: Decoder[IssuesEvent] = deriveDecoder

  // MemberEvent decoder (manual to map nested fields)
  implicit val memberEventDecoder: Decoder[MemberEvent] = Decoder.instance { c: HCursor =>
    for {
      idStr      <- c.get[String]("id")
      actor      <- c.get[User]("actor")
      repo       <- c.get[Repository]("repo")
      payload    = c.downField("payload")
      member     <- payload.get[User]("member")
      action     <- payload.get[String]("action")
      createdStr <- c.get[String]("created_at").orElse(c.get[String]("createdAt"))
      createdAt   = GitHubEvent.parseInstant(createdStr)
      org        <- c.get[Organization]("org")
    } yield MemberEvent(idStr, actor, repo, member, action, createdAt, org)
  }

  // Top-level GitHubEvent decoder
  implicit val githubEventDecoder: Decoder[GitHubEvent] = Decoder.instance { c =>
    c.get[String]("type").flatMap {
      case "PushEvent"    => pushEventDecoder(c)
      case "WatchEvent"   => watchEventDecoder(c)
      case "CreateEvent"  => createEventDecoder(c)
      case "IssuesEvent"  => issuesEventDecoder(c)
      case "MemberEvent"  => memberEventDecoder(c)
      case other           => Left(DecodingFailure(s"Unsupported event type: $other", c.history))
    }
  }
}
