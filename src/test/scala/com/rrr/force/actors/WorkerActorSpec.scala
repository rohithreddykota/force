package com.rrr.force.actors

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.rrr.force.models._
import com.rrr.force.actors.WorkerMessage._
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers

import java.time.Instant

class WorkerActorSpec
    extends TestKit(ActorSystem("WorkerActorSpec"))
        with AnyWordSpecLike
        with Matchers
        with BeforeAndAfterAll {

    override def afterAll(): Unit = {
        TestKit.shutdownActorSystem(system)
    }

    "WorkerActor" should {

        "return filtered query results" in {
            val probe = TestProbe()
            val user = User(1, "a1", Some("Actor One"), Some("actor1@example.com"))
            val repo = Repository(1, "force-repo", user)
            val now = Instant.now

            val data = Seq(
                PushEvent("e1", user, repo, PushPayload("refs/heads/main", "abc123", "def456", Seq(Commit("c1", "Fix things", "url1"))), now),
                PushEvent("e2", user.copy(login = "a2"), repo, PushPayload("refs/heads/main", "abc123", "def456", Seq(Commit("c2", "Other", "url2"))), now)
            )

            val query = Query(actor = Some("a1"))
            val worker = system.actorOf(WorkerActor.props(partitionId = 0, partitionData = data))

            probe.send(worker, ProcessQuery(query, None))
            val QueryResult(result) = probe.expectMsgType[QueryResult]
            result.size shouldBe 1
            result.head.id shouldBe "e1"
        }

        "perform enrichment with broadcast data" in {
            val probe = TestProbe()
            val user = User(1, "a1", Some("Old Name"), Some("actor1@example.com"))
            val enrichedUser = User(1, "a1", Some("New Name"), Some("actor1@example.com"))
            val repo = Repository(1, "force-repo", user)
            val now = Instant.now

            val data = Seq(
                PushEvent("e1", user, repo, PushPayload("refs/heads/main", "abc123", "def456", Seq(Commit("c1", "Test", "url1"))), now)
            )

            val broadcast = BroadcastData(users = Seq(enrichedUser), organizations = Seq.empty)
            val query = Query()

            val worker = system.actorOf(WorkerActor.props(0, data))
            probe.send(worker, ProcessQuery(query, Some(broadcast)))
            val QueryResult(result) = probe.expectMsgType[QueryResult]

            result.head.actor.name shouldBe Some("New Name")
        }
    }
}