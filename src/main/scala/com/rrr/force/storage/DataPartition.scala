// src/main/scala/com/rrr/force/storage/DataPartition.scala
package com.rrr.force.storage

import com.rrr.force.domain._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._

/**
 * Represents an immutable data partition loaded from disk.
 *
 * @param id     Partition identifier
 * @param events Sequence of GitHubEvent instances in this partition
 */
case class DataPartition(id: Int, events: Seq[GitHubEvent])

object DataPartition {
  // ----- Circe Decoders -----
  implicit val userDecoder: Decoder[User] = deriveDecoder
  implicit val repositoryDecoder: Decoder[Repository] = deriveDecoder
  implicit val commitDecoder: Decoder[Commit] = deriveDecoder
  implicit val pushEventDecoder: Decoder[PushEvent] = deriveDecoder
  implicit val watchEventDecoder: Decoder[WatchEvent] = deriveDecoder
  implicit val createEventDecoder: Decoder[CreateEvent] = deriveDecoder
  implicit val issueDetailsDecoder: Decoder[IssueDetails] = deriveDecoder
  implicit val issuesEventDecoder: Decoder[IssuesEvent] = deriveDecoder

  // Dispatch on the "type" field
  implicit val gitHubEventDecoder: Decoder[GitHubEvent] = Decoder.instance { cursor =>
    cursor.get[String]("type").flatMap {
      case "PushEvent" => pushEventDecoder(cursor)
      case "WatchEvent" => watchEventDecoder(cursor)
      case "CreateEvent" => createEventDecoder(cursor)
      case "IssuesEvent" => issuesEventDecoder(cursor)
      case other => Left(DecodingFailure(s"Unknown event type: '$other'", cursor.history))
    }
  }

  /**
   * Loads partition file from disk, expecting JSON array named partition_<id>.json under `path`.
   * Throws on file I/O or JSON/parsing errors.
   */
  def load(path: String, id: Int): DataPartition = {
    val filePath = s"$path/partition_$id.json"
    val file = new java.io.File(filePath)
    if (!file.exists())
      throw new java.io.FileNotFoundException(s"Partition file not found: $filePath")

    val rawJson = scala.io.Source.fromFile(file)(scala.io.Codec.UTF8).mkString
    // Parse raw JSON string into a JSON value
    val jsonArr = parse(rawJson) match {
      case Left(err) => throw new RuntimeException(s"Invalid JSON in $filePath: ${err.message}")
      case Right(value) => value
    }

    // Decode JSON array into Seq[GitHubEvent]
    jsonArr.as[Seq[GitHubEvent]] match {
      case Left(err) => throw new RuntimeException(s"Decoding error in $filePath: ${err.message}")
      case Right(events) => DataPartition(id, events)
    }
  }
}
