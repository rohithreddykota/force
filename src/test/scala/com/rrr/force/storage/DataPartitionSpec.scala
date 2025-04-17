// src/test/scala/com/rrr/force/storage/DataPartitionSpec.scala
package com.rrr.force.storage

import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, StandardOpenOption}
import java.nio.charset.StandardCharsets
import com.rrr.force.domain._
import java.time.Instant

class DataPartitionSpec extends AnyFunSuite {
  test("DataPartition.load should read JSON file and parse all event types") {
    // 1. Create temp directory and file
    val tmpDir  = Files.createTempDirectory("dp-test")
    val file    = tmpDir.resolve("partition_0.json")

    // 2. Sample JSON with PushEvent, WatchEvent, CreateEvent, IssuesEvent
    val nowStr  = "2025-01-01T00:00:00Z"
    val json =
      s"""[
         |  {
         |    "id": "1",
         |    "type": "PushEvent",
         |    "actor": { "id": 10, "login": "user1", "displayLogin": "user1", "avatarUrl": "url" },
         |    "repo":  { "id": 100, "name": "repo1" },
         |    "payload": {
         |      "size": 1, "distinct_size": 1,
         |      "ref": "refs/heads/main", "head": "abc", "before": "def",
         |      "commits": [ { "sha": "abc", "author": { "name": "n", "email": "e" }, "message": "msg", "url": "u" } ]
         |    },
         |    "created_at": "$nowStr"
         |  },
         |  {
         |    "id": "2",
         |    "type": "WatchEvent",
         |    "actor": { "id": 20, "login": "user2", "displayLogin": null, "avatarUrl": "url2" },
         |    "repo":  { "id": 200, "name": "repo2" },
         |    "payload": { "action": "started" },
         |    "created_at": "$nowStr"
         |  },
         |  {
         |    "id": "3",
         |    "type": "CreateEvent",
         |    "actor": { "id": 30, "login": "user3", "displayLogin": "u3", "avatarUrl": "url3" },
         |    "repo":  { "id": 300, "name": "repo3" },
         |    "payload": { "ref": "dev", "ref_type": "branch", "master_branch": "main", "description": "desc" },
         |    "created_at": "$nowStr"
         |  },
         |  {
         |    "id": "4",
         |    "type": "IssuesEvent",
         |    "actor": { "id": 40, "login": "user4", "displayLogin": "u4", "avatarUrl": "url4" },
         |    "repo":  { "id": 400, "name": "repo4" },
         |    "payload": {
         |      "action": "opened",
         |      "issue": { "number": 5, "title": "t", "state": "open", "created_at": "$nowStr", "updated_at": "$nowStr" }
         |    },
         |    "created_at": "$nowStr"
         |  }
         |]""".stripMargin

    Files.write(file, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)

    // 3. Load and verify
    val dp = DataPartition.load(tmpDir.toString, 0)
    assert(dp.id == 0)
    assert(dp.events.size == 4)

    // PushEvent
    dp.events(0) match {
      case pe: PushEvent =>
        assert(pe.id == "1")
        assert(pe.actor.login == "user1")
        assert(pe.repo.name == "repo1")
        assert(pe.commits.head.sha == "abc")
        assert(pe.createdAt == Instant.parse(nowStr))
      case other => fail(s"Expected PushEvent, got: $other")
    }
    // WatchEvent
    dp.events(1) match {
      case we: WatchEvent =>
        assert(we.id == "2")
        assert(we.actor.login == "user2")
        assert(we.action == "started")
      case other => fail(s"Expected WatchEvent, got: $other")
    }
    // CreateEvent
    dp.events(2) match {
      case ce: CreateEvent =>
        assert(ce.id == "3")
        assert(ce.ref.contains("dev"))
        assert(ce.refType == "branch")
        assert(ce.description.contains("desc"))
      case other => fail(s"Expected CreateEvent, got: $other")
    }
    // IssuesEvent
    dp.events(3) match {
      case ie: IssuesEvent =>
        assert(ie.id == "4")
        assert(ie.issue.number == 5)
        assert(ie.action == "opened")
      case other => fail(s"Expected IssuesEvent, got: $other")
    }
  }
}
