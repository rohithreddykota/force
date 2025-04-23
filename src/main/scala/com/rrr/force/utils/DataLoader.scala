package com.rrr.force.utils

import io.circe.parser.decode
import com.rrr.force.domain.GitHubEvent
import com.rrr.force.domain.GitHubDecoders._
import scala.io.Source

object DataLoader {

  def loadPartition(path: String, id: Int, total: Int): Seq[GitHubEvent] = {
    Source.fromFile(path)("UTF-8")
      .getLines()
      .zipWithIndex
      .collect {
        case (line, idx) if idx % total == id =>
          decode[GitHubEvent](line) match {
            case Right(evt) => evt
            case Left(err)  => throw new RuntimeException(s"Parsing line $idx failed: $err")
          }
      }
      .toSeq
  }

  def loadGitHubEvents(path: String): Seq[GitHubEvent] = {
    val raw = Source.fromFile(path)("UTF-8").mkString
    decode[Seq[GitHubEvent]](raw) match {
      case Right(events) => events
      case Left(err) =>
        throw new RuntimeException(s"Failed to decode GitHubEvent sequence from $path: $err")
    }
  }


  def loadGitHubEventsFromResource(resourceName: String): Seq[GitHubEvent] = {
    val stream = getClass.getClassLoader.getResourceAsStream(resourceName)
    if (stream == null)
      throw new RuntimeException(s"Resource not found on classpath: $resourceName")

    val raw = Source.fromInputStream(stream)("UTF-8").mkString
    stream.close()
    decode[Seq[GitHubEvent]](raw) match {
      case Right(events) => events
      case Left(err) =>
        throw new RuntimeException(s"Failed to decode GitHubEvent sequence from resource $resourceName: $err")
    }
  }
}
