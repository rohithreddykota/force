package com.rrr.force.query

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import com.rrr.force.models._

class DefaultQueryProcessorSpec extends AnyWordSpec with Matchers {

    val user: User = User(1, "u1", Some("ghuser"), Some("ghuser@example.com"))
    val repo: Repository = Repository(1, "test-repo", user)
    val now: Instant = Instant.now

    val events: Seq[GitHubEvent] = Seq(
        PushEvent("1", user, repo, PushPayload("refs/heads/main", "abc123", "def456", Seq(Commit("c1", "Fix bug", "url1"))), now),
        PushEvent("2", user, repo, PushPayload("refs/heads/main", "abc123", "def456", Seq(Commit("c2", "Add test", "url2"))), now.minusSeconds(86400))
    )

    "DefaultQueryProcessor" should {

        "filter by actor login" in {
            val query = Query(actor = Some("u1"))
            val result = DefaultQueryProcessor.process(query, events, None)
            result.size shouldBe 2
        }

        "filter by date range" in {
            val query = Query(dateRange = Some((now.minusSeconds(1000), now.plusSeconds(1000))))
            val result = DefaultQueryProcessor.process(query, events, None)
            result.map(_.id) should contain("1")
            result.map(_.id) should not contain "2"
        }

        "filter by commit message" in {
            val query = Query(additionalFilters = Map("commitMessageContains" -> "test"))
            val result = DefaultQueryProcessor.process(query, events, None)
            result.size shouldBe 1
            result.head.id shouldBe "2"
        }

        "enrich actor using broadcast data" in {
            val enrichedUser = user.copy(name = Some("ghuser Enriched"))
            val broadcast = BroadcastData(users = Seq(enrichedUser), organizations = Seq.empty)

            val result = DefaultQueryProcessor.process(Query(), events, Some(broadcast))
            result.forall(_.actor.name.contains("ghuser Enriched")) shouldBe true
        }
    }
}