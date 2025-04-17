// src/main/scala/com/rrr/force/storage/DataPartition.scala
package com.rrr.force.storage

import cats.implicits.catsSyntaxApplicativeError
import com.rrr.force.domain._
import io.circe._
import io.circe.parser._

import scala.io.Source

/**
 * Immutable data partition loaded from a JSON file on disk.
 *
 * @param id     Partition identifier
 * @param events Sequence of GitHubEvent instances in this partition
 */
case class DataPartition(id: Int, events: Seq[GitHubEvent])

object DataPartition {

  /**
   * Load partition from JSON file named partition_<id>.json under given directory.
   *
   * @param path Directory containing partition files
   * @param id   Partition identifier
   * @return     DataPartition instance with parsed events
   */
  def load(path: String, id: Int): DataPartition = {
    val filePath = s"$path/partition_$id.json"
    val file = new java.io.File(filePath)
    if (!file.exists()) {
      throw new java.io.FileNotFoundException(s"Partition file not found: $filePath")
    }

    // Read raw JSON
    val raw = Source.fromFile(file)(scala.io.Codec.UTF8).mkString
    val json = parse(raw).getOrElse(
      throw new RuntimeException(s"Invalid JSON in $filePath")
    )

    // === Circe decoders for nested models ===

    // Decode User from {"id", "login", "displayLogin", "avatarUrl"}
    implicit val userDecoder: Decoder[User] = Decoder.instance { c =>
      for {
        idVal        <- c.get[Long]("actor.id").orElse(c.get[Long]("id"))
        login        <- c.get[String]("actor.login").orElse(c.get[String]("login"))
        displayLogin <- c.get[Option[String]]("actor.displayLogin").orElse(c.get[Option[String]]("displayLogin"))
        avatarUrl    <- c.get[String]("actor.avatarUrl").orElse(c.get[String]("avatarUrl"))
      } yield User(idVal, login, displayLogin, avatarUrl)
    }

    // Decode Repository from {"id", "name"}
    implicit val repoDecoder: Decoder[Repository] = Decoder.instance { c =>
      for {
        idVal <- c.get[Long]("repo.id").orElse(c.get[Long]("id"))
        name  <- c.get[String]("repo.name").orElse(c.get[String]("name"))
      } yield Repository(idVal, name)
    }

    // Decode Commit from {"sha", "author": {"name","email"}, "message","url"}
    implicit val commitDecoder: Decoder[Commit] = Decoder.instance { c =>
      for {
        sha    <- c.get[String]("sha")
        author = c.downField("author")
        name   <- author.get[String]("name")
        email  <- author.get[String]("email")
        msg    <- c.get[String]("message")
        url    <- c.get[String]("url")
      } yield Commit(sha, name, email, msg, url)
    }

    // Decode PushEvent from full structure
    implicit val pushDecoder: Decoder[PushEvent] = Decoder.instance { c =>
      for {
        idStr      <- c.get[String]("id")
        actor      <- c.get[User]("actor")
        repo       <- c.get[Repository]("repo")
        payload     = c.downField("payload")
        size       <- payload.get[Int]("size")
        distinct   <- payload.get[Int]("distinct_size")
        ref        <- payload.get[String]("ref")
        head       <- payload.get[String]("head")
        before     <- payload.get[String]("before")
        commits    <- payload.downField("commits").as[Seq[Commit]]
        createdStr <- c.get[String]("created_at")
        createdAt   = GitHubEvent.parseInstant(createdStr)
      } yield PushEvent(idStr, actor, repo, size, distinct, ref, head, before, commits, createdAt)
    }

    // Top‐level dispatcher on "type" field
    implicit val eventDecoder: Decoder[GitHubEvent] = Decoder.instance { c =>
      c.get[String]("type").flatMap {
        case "PushEvent" => pushDecoder(c)
        case t           => Left(DecodingFailure(s"Unsupported event type: $t", c.history))
      }
    }

    // Decode array of GitHubEvent
    val events: Seq[GitHubEvent] = json.as[Seq[GitHubEvent]].fold(
      df  => throw new RuntimeException(s"Decoding error in $filePath: ${df.message}"),
      seq => seq
    )

    DataPartition(id, events)
  }
}
