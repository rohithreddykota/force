// src/test/scala/com/rrr/force/broadcast/BroadcastDataSpec.scala
package com.rrr.force.broadcast

import java.nio.file.Files
import com.typesafe.config.ConfigFactory
import com.rrr.force.domain.{BroadcastUser, Organization}
import com.rrr.force.broadcast.BroadcastData
import org.scalatest.funsuite.AnyFunSuite
import io.circe.generic.auto._

class BroadcastDataSpec extends AnyFunSuite {
  private def writeTempJson(content: String) = {
    val tmp = Files.createTempFile("bd-test", ".json")
    Files.write(tmp, content.getBytes("UTF-8"))
    tmp.toAbsolutePath.toString
  }

  test("BroadcastData.load should parse BroadcastUser and Organization") {
    // Prepare JSON arrays
    val usersJson =
      """[
        |{ "id":1, "login":"alice", "name":"Alice", "email":"alice@example.com" },
        |{ "id":2, "login":"bob",   "name":null,    "email":null }
        |]""".stripMargin
    val orgsJson =
      """[
        |{ "id":100, "login":"orgA", "name":"Org A" },
        |{ "id":101, "login":"orgB", "name":null }
        |]""".stripMargin

    val usersPath = writeTempJson(usersJson)
    val orgsPath  = writeTempJson(orgsJson)

    // Build in-memory Config
    val confStr =
      s"""
         |force.broadcast.users-file = "$usersPath"
         |force.broadcast.orgs-file  = "$orgsPath"
       """.stripMargin
    val config = ConfigFactory.parseString(confStr).withFallback(ConfigFactory.load())

    // Execute
    val bd = BroadcastData.load(config)

    // Assertions
    assert(bd.users == Seq(
      BroadcastUser(1, "alice", Some("Alice"), Some("alice@example.com")),
      BroadcastUser(2, "bob",   None,           None)
    ))

    assert(bd.orgs == Seq(
      Organization(100, "orgA", Some("Org A")),
      Organization(101, "orgB", None)
    ))
  }
}
