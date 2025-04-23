// src/main/scala/com/rrr/force/storage/DataPartition.scala
package com.rrr.force.storage

import cats.implicits.catsSyntaxApplicativeError
import com.rrr.force.domain._
import io.circe.DecodingFailure
import io.circe.parser.parse

import scala.io.Source

/**
 * Immutable data partition loaded from an NDJSON file,
 * where each line is a separate GitHubEvent object.
 */
case class DataPartition(id: Int, events: Seq[GitHubEvent])

object DataPartition {

  // --- Domain decoders ---

  implicit val commitDecoder: io.circe.Decoder[Commit] = io.circe.Decoder.instance { c =>
    for {
      sha <- c.get[String]("sha")
      author = c.downField("author")
      name <- author.get[String]("name")
      email <- author.get[String]("email")
      msg <- c.get[String]("message")
      url <- c.get[String]("url")
    } yield Commit(sha, name, email, msg, url)
  }

  implicit val userDecoder: io.circe.Decoder[User] = io.circe.Decoder.instance { c =>
    for {
      idVal <- c.get[Long]("id")
      login <- c.get[String]("login")
      displayLogin <- c.get[Option[String]]("displayLogin").orElse(c.get[Option[String]]("display_login"))
      avatarUrl <- c.get[String]("avatarUrl").orElse(c.get[String]("avatar_url"))
    } yield User(idVal, login, displayLogin, avatarUrl)
  }

  implicit val repoDecoder: io.circe.Decoder[Repository] = io.circe.Decoder.instance { c =>
    for {
      idVal <- c.get[Long]("id")
      name <- c.get[String]("name")
    } yield Repository(idVal, name)
  }

  implicit val pushDecoder: io.circe.Decoder[PushEvent] = io.circe.Decoder.instance { c =>
    for {
      idStr <- c.get[String]("id")
      actor <- c.get[User]("actor")
      repo <- c.get[Repository]("repo")
      pd = c.downField("payload")
      size <- pd.get[Int]("size")
      distinct <- pd.get[Int]("distinct_size")
      ref <- pd.get[String]("ref")
      head <- pd.get[String]("head")
      before <- pd.get[String]("before")
      commits <- pd.downField("commits").as[Seq[Commit]]
      createdStr <- c.get[String]("created_at")
      createdAt = GitHubEvent.parseInstant(createdStr)
    } yield PushEvent(idStr, actor, repo, size, distinct, ref, head, before, commits, createdAt)
  }

  implicit val watchDecoder: io.circe.Decoder[WatchEvent] = io.circe.Decoder.instance { c =>
    for {
      idStr <- c.get[String]("id")
      actor <- c.get[User]("actor")
      repo <- c.get[Repository]("repo")
      action <- c.downField("payload").get[String]("action")
      createdStr <- c.get[String]("created_at")
      createdAt = GitHubEvent.parseInstant(createdStr)
    } yield WatchEvent(idStr, actor, repo, action, createdAt)
  }

  implicit val createDecoder: io.circe.Decoder[CreateEvent] = io.circe.Decoder.instance { c =>
    val pd = c.downField("payload")
    for {
      idStr <- c.get[String]("id")
      actor <- c.get[User]("actor")
      repo <- c.get[Repository]("repo")
      refOpt <- pd.get[Option[String]]("ref")
      refType <- pd.get[String]("ref_type")
      masterOpt <- pd.get[Option[String]]("master_branch")
      descOpt <- pd.get[Option[String]]("description")
      createdStr <- c.get[String]("created_at")
      createdAt = GitHubEvent.parseInstant(createdStr)
    } yield CreateEvent(idStr, actor, repo, refOpt, refType, masterOpt, descOpt, createdAt)
  }

  implicit val issueDetDecoder: io.circe.Decoder[IssueDetails] = io.circe.Decoder.instance { c =>
    for {
      number <- c.get[Long]("number")
      title <- c.get[String]("title")
      state <- c.get[String]("state")
      createdStr <- c.get[String]("created_at")
      updatedStr <- c.get[String]("updated_at")
      createdAt = GitHubEvent.parseInstant(createdStr)
      updatedAt = GitHubEvent.parseInstant(updatedStr)
    } yield IssueDetails(number, title, state, createdAt, updatedAt)
  }

  implicit val issuesDecoder: io.circe.Decoder[IssuesEvent] = io.circe.Decoder.instance { c =>
    val pd = c.downField("payload")
    for {
      idStr <- c.get[String]("id")
      actor <- c.get[User]("actor")
      repo <- c.get[Repository]("repo")
      action <- pd.get[String]("action")
      issue <- pd.get[IssueDetails]("issue")
      createdStr <- c.get[String]("created_at")
      createdAt = GitHubEvent.parseInstant(createdStr)
    } yield IssuesEvent(idStr, actor, repo, action, issue, createdAt)
  }

  implicit val eventDecoder: io.circe.Decoder[GitHubEvent] = io.circe.Decoder.instance { c =>
    c.get[String]("type").flatMap {
      case "PushEvent" => pushDecoder(c)
      case "WatchEvent" => watchDecoder(c)
      case "CreateEvent" => createDecoder(c)
      case "IssuesEvent" => issuesDecoder(c)
      case other => Left(DecodingFailure(s"Unsupported event type: $other", c.history))
    }
  }

  /**
   * Load partition_<id>.json from `path` directory in NDJSON format.
   * Each line is a standalone GitHubEvent JSON.
   */
  def load(path: String, id: Int): DataPartition = {
    val filePath = s"$path/partition_$id.json"
    val file = new java.io.File(filePath)
    if (!file.exists())
      throw new java.io.FileNotFoundException(s"Partition file not found: $filePath")

    val source = Source.fromFile(file)(scala.io.Codec.UTF8)
    try {
      val events = source
        .getLines()
        .zipWithIndex
        .map { case (line, idx) =>
          parse(line) match {
            case Left(err) =>
              throw new RuntimeException(
                s"JSON parse error in $filePath at line ${idx + 1}: ${err.message}"
              )
            case Right(json) =>
              json.as[GitHubEvent] match {
                case Left(decErr) =>
                  throw new RuntimeException(
                    s"JSON decode error in $filePath at line ${idx + 1}: ${decErr.message}"
                  )
                case Right(evt) => evt
              }
          }
        }
        .toList

      DataPartition(id, events)
    } finally {
      source.close()
    }
  }

  def fromRecords(id: Int, events: Seq[GitHubEvent]): DataPartition =
    DataPartition(id, events)
}
