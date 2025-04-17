// src/main/scala/com/rrr/force/broadcast/BroadcastData.scala
package com.rrr.force.broadcast

import com.rrr.force.domain.{BroadcastUser, Organization}
import com.typesafe.config.Config
import io.circe._, io.circe.parser._, io.circe.generic.semiauto._
import com.rrr.force.utils.DefaultConfigParser

/**
 * Container for small dimension tables broadcast to all workers.
 * @param users Sequence of broadcast users for joins/enrichment
 * @param orgs  Sequence of broadcast organizations for joins/enrichment
 */
case class BroadcastData(
                          users: Seq[BroadcastUser],
                          orgs: Seq[Organization]
                        )

object BroadcastData {
  // Config keys for broadcast file paths
  private val UsersFileKey = "force.broadcast.users-file"
  private val OrgsFileKey  = "force.broadcast.orgs-file"

  // Shared application config
  private val config: Config = DefaultConfigParser.config

  // Circe decoders for broadcast models
  implicit val userDecoder: Decoder[BroadcastUser] = deriveDecoder
  implicit val orgDecoder: Decoder[Organization]  = deriveDecoder

  /**
   * Load BroadcastData using default application.conf
   */
  def load(): BroadcastData = load(config)

  /**
   * Load BroadcastData from provided Config
   * @param cfg Config containing keys UsersFileKey and OrgsFileKey
   */
  def load(cfg: Config): BroadcastData = {
    // Validate config presence
    Seq(UsersFileKey, OrgsFileKey).foreach { key =>
      if (!cfg.hasPath(key))
        throw new IllegalStateException(s"Missing config key: $key")
    }

    val usersPath = cfg.getString(UsersFileKey)
    val orgsPath  = cfg.getString(OrgsFileKey)

    val usersJson = scala.io.Source.fromFile(usersPath)(scala.io.Codec.UTF8).mkString
    val orgsJson  = scala.io.Source.fromFile(orgsPath)(scala.io.Codec.UTF8).mkString

    val users = parse(usersJson) match {
      case Left(err)    => throw new RuntimeException(s"Invalid JSON in $usersPath: ${err.message}")
      case Right(json)  => json.as[Seq[BroadcastUser]].fold(
        df => throw new RuntimeException(s"Decoding users failed: ${df.message}"), identity
      )
    }

    val orgs = parse(orgsJson) match {
      case Left(err)    => throw new RuntimeException(s"Invalid JSON in $orgsPath: ${err.message}")
      case Right(json)  => json.as[Seq[Organization]].fold(
        df => throw new RuntimeException(s"Decoding orgs failed: ${df.message}"), identity
      )
    }

    BroadcastData(users, orgs)
  }

  val empty: BroadcastData = BroadcastData(Seq.empty, Seq.empty)
}
