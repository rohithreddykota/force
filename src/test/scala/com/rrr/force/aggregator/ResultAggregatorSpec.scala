// src/test/scala/com/rrr/force/aggregator/ResultAggregatorSpec.scala
package com.rrr.force.aggregator

import org.scalatest.funsuite.AnyFunSuite
import com.rrr.force.domain._
import com.rrr.force.aggregator.ResultAggregator
import java.time.Instant

class ResultAggregatorSpec extends AnyFunSuite {
  test("merge should combine simple partial results by key override") {
    val row1 = Map("a" -> 1, "b" -> 2)
    val row2 = Map("b" -> 3, "c" -> 4)
    val p1 = PartialResult(0, Seq(row1))
    val p2 = PartialResult(1, Seq(row2))

    val finalRes = ResultAggregator.merge(Seq(p1, p2))
    assert(finalRes.data.size == 1)
    val merged = finalRes.data.head
    // 'a' from row1, 'b' overridden by row2, 'c' from row2
    assert(merged("a") == 1)
    assert(merged("b") == 3)
    assert(merged("c") == 4)
  }

  test("merge of empty partials yields empty FinalResult") {
    val finalRes = ResultAggregator.merge(Seq.empty)
    assert(finalRes.data.isEmpty)
  }
}
