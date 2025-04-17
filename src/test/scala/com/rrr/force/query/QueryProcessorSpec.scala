// src/test/scala/com/rrr/force/query/QueryProcessorSpec.scala
package com.rrr.force.query

import com.rrr.force.broadcast.BroadcastData
import com.rrr.force.domain._
import com.rrr.force.storage.DataPartition
import org.scalatest.funsuite.AnyFunSuite

import java.time.Instant

class QueryProcessorSpec extends AnyFunSuite {
  test("QueryProcessor returns correct aggregation") {
    val now = Instant.parse("2025-01-01T00:00:00Z")
    val ev1 = PushEvent("1", User(1, "u1", None, ""), Repository(1, "r"), 1, 1, "", "", "", Seq(), now)
    val ev2 = PushEvent("2", User(2, "u2", None, ""), Repository(1, "r"), 1, 1, "", "", "", Seq(), now)
    val dp = DataPartition(0, Seq(ev1, ev2))
    val bc = BroadcastData.empty

    val json =
      """{
        | "filters":[{"type":"Eq","field":"eventType","value":"PushEvent"}],
        | "groupBy":["ByRepo"],
        | "aggregations":[{"field":"count","op":"CountOp"}]
        |}""".stripMargin

    val res = QueryProcessor.run(json, Seq(dp), bc)
    assert(res.isRight)
    val FinalResult(data) = res.toOption.get
    assert(data.head("repo") == "r")
    assert(data.head("count") == 2)
  }
}
