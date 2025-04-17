// src/test/scala/com/rrr/force/storage/DataPartitionSpec.scala
package com.rrr.force.storage

import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, StandardOpenOption}
import java.nio.charset.StandardCharsets
import com.rrr.force.domain.{GitHubEvent, PushEvent}
import java.time.Instant

class DataPartitionSpec extends AnyFunSuite {
  test("DataPartition.load should read JSON file and parse PushEvent") {
    // 1. Create temp directory and file
    val tmpDir  = Files.createTempDirectory("dp-test")
    val file    = tmpDir.resolve("partition_0.json")
    val nowStr  = "2025-01-01T00:00:00Z"

    // 2. Sample JSON with one PushEvent
    val json =
      s"""[
         |  {
         |    "id": "1",
         |    "type": "PushEvent",
         |    "actor": {
         |      "id": 10,
         |      "login": "user1",
         |      "displayLogin": "user1",
         |      "avatarUrl": "url"
         |    },
         |    "repo": {
         |      "id": 100,
         |      "name": "repo1"
         |    },
         |    "payload": {
         |      "size": 1,
         |      "distinct_size": 1,
         |      "ref": "refs/heads/main",
         |      "head": "abc",
         |      "before": "def",
         |      "commits": [
         |        {
         |          "sha": "abc",
         |          "author": { "name": "n", "email": "e" },
         |          "message": "msg",
         |          "url": "u"
         |        }
         |      ]
         |    },
         |    "created_at": "$nowStr"
         |  }
         |]
         |""".stripMargin

    Files.write(file, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)

    // 3. Load and verify
    val dp = DataPartition.load(tmpDir.toString, 0)
    assert(dp.id == 0)
    assert(dp.events.nonEmpty)

    dp.events.head match {
      case pe: PushEvent =>
        assert(pe.id == "1")
        assert(pe.actor.login == "user1")
        assert(pe.repo.name == "repo1")
        assert(pe.commits.head.sha == "abc")
        assert(pe.createdAt == Instant.parse(nowStr))
      case other =>
        fail(s"Expected PushEvent, but got: $other")
    }
  }
}
