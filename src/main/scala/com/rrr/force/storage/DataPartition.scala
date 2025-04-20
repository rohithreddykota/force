// src/main/scala/com/rrr/force/storage/DataPartition.scala
package com.rrr.force.storage

import cats.implicits.catsSyntaxApplicativeError
import com.rrr.force.domain._
import io.circe._
import io.circe.parser._

import scala.io.Source

/**
 * Immutable data partition loaded from a JSON file containing an array of GitHubEvent objects.
 */
case class DataPartition(id: Int, events: Seq[GitHubEvent])

object DataPartition {
  def load(path: String, id: Int): DataPartition = {
    val filePath = s"$path/partition_$id.json"
    val file = new java.io.File(filePath)
    if (!file.exists()) throw new java.io.FileNotFoundException(s"Partition file not found: $filePath")

    val raw = Source.fromFile(file)(scala.io.Codec.UTF8).mkString
    val json = parse(raw).getOrElse(throw new RuntimeException(s"Invalid JSON in $filePath"))

    // Manual decoders for each event type
    implicit val commitDecoder: Decoder[Commit] = Decoder.instance { c =>
      for {
        sha   <- c.get[String]("sha")
        author = c.downField("author")
        name  <- author.get[String]("name")
        email <- author.get[String]("email")
        msg   <- c.get[String]("message")
        url   <- c.get[String]("url")
      } yield Commit(sha, name, email, msg, url)
    }

    implicit val userDecoder: Decoder[User] = Decoder.instance { c =>
      for {
        idVal        <- c.get[Long]("id")
        login        <- c.get[String]("login")
        displayLogin <- c.get[Option[String]]("displayLogin").orElse(c.get[Option[String]]("display_login"))
        avatarUrl    <- c.get[String]("avatarUrl").orElse(c.get[String]("avatar_url"))
      } yield User(idVal, login, displayLogin, avatarUrl)
    }

    implicit val repoDecoder: Decoder[Repository] = Decoder.instance { c =>
      for {
        idVal <- c.get[Long]("id")
        name  <- c.get[String]("name")
      } yield Repository(idVal, name)
    }

    implicit val pushDecoder: Decoder[PushEvent] = Decoder.instance { c =>
      for {
        idStr      <- c.get[String]("id")
        actor      <- c.get[User]("actor")
        repo       <- c.get[Repository]("repo")
        pd         = c.downField("payload")
        size       <- pd.get[Int]("size")
        distinct   <- pd.get[Int]("distinct_size")
        ref        <- pd.get[String]("ref")
        head       <- pd.get[String]("head")
        before     <- pd.get[String]("before")
        commits    <- pd.downField("commits").as[Seq[Commit]]
        createdStr <- c.get[String]("created_at")
        createdAt   = GitHubEvent.parseInstant(createdStr)
      } yield PushEvent(idStr, actor, repo, size, distinct, ref, head, before, commits, createdAt)
    }

    implicit val watchDecoder: Decoder[WatchEvent] = Decoder.instance { c =>
      for {
        idStr      <- c.get[String]("id")
        actor      <- c.get[User]("actor")
        repo       <- c.get[Repository]("repo")
        action     <- c.downField("payload").get[String]("action")
        createdStr <- c.get[String]("created_at")
        createdAt   = GitHubEvent.parseInstant(createdStr)
      } yield WatchEvent(idStr, actor, repo, action, createdAt)
    }

    implicit val createDecoder: Decoder[CreateEvent] = Decoder.instance { c =>
      val pd = c.downField("payload")
      for {
        idStr      <- c.get[String]("id")
        actor      <- c.get[User]("actor")
        repo       <- c.get[Repository]("repo")
        refOpt     <- pd.get[Option[String]]("ref")
        refType    <- pd.get[String]("ref_type")
        masterOpt  <- pd.get[Option[String]]("master_branch")
        descOpt    <- pd.get[Option[String]]("description")
        createdStr <- c.get[String]("created_at")
        createdAt   = GitHubEvent.parseInstant(createdStr)
      } yield CreateEvent(idStr, actor, repo, refOpt, refType, masterOpt, descOpt, createdAt)
    }

    implicit val issueDetDecoder: Decoder[IssueDetails] = Decoder.instance { c =>
      for {
        number     <- c.get[Long]("number")
        title      <- c.get[String]("title")
        state      <- c.get[String]("state")
        createdStr <- c.get[String]("created_at")
        updatedStr <- c.get[String]("updated_at")
        createdAt   = GitHubEvent.parseInstant(createdStr)
        updatedAt   = GitHubEvent.parseInstant(updatedStr)
      } yield IssueDetails(number, title, state, createdAt, updatedAt)
    }

    implicit val issuesDecoder: Decoder[IssuesEvent] = Decoder.instance { c =>
      val pd = c.downField("payload")
      for {
        idStr      <- c.get[String]("id")
        actor      <- c.get[User]("actor")
        repo       <- c.get[Repository]("repo")
        action     <- pd.get[String]("action")
        issue      <- pd.get[IssueDetails]("issue")
        createdStr <- c.get[String]("created_at")
        createdAt   = GitHubEvent.parseInstant(createdStr)
      } yield IssuesEvent(idStr, actor, repo, action, issue, createdAt)
    }

    implicit val eventDecoder: Decoder[GitHubEvent] = Decoder.instance { c =>
      c.get[String]("type").flatMap {
        case "PushEvent"   => pushDecoder(c)
        case "WatchEvent"  => watchDecoder(c)
        case "CreateEvent" => createDecoder(c)
        case "IssuesEvent" => issuesDecoder(c)
        case other           => Left(DecodingFailure(s"Unsupported event type: $other", c.history))
      }
    }

    val events = json.as[Seq[GitHubEvent]].fold(
      err => throw new RuntimeException(s"Decoding error in $filePath: ${err.message}"),
      seq => seq
    )
    DataPartition(id, events)
  }

  def fromRecords(id: Int, events: Seq[GitHubEvent]): DataPartition =
    DataPartition(id, events)
}
