package com.rrr.force.aggregation

import org.scalatest.funsuite.AnyFunSuite

class AggregationInstancesSpec extends AnyFunSuite {
  import Instances._

  test("SumOpLong accumulates and finishes correctly with Ints and Longs") {
    val inputs: Seq[Any] = Seq(1, 2L, "3")
    val sumAcc = inputs.foldLeft(SumOpLong.zero)(SumOpLong.accumulate)
    val result = SumOpLong.finish(sumAcc)
    assert(result == 6L)
  }

  test("SumOpLong throws on unsupported type") {
    assertThrows[IllegalArgumentException] {
      SumOpLong.accumulate(SumOpLong.zero, 1.5)
    }
  }

  test("CountOp counts each element regardless of type") {
    val inputs: Seq[Any] = Seq(1, "x", 3L, ())
    val countAcc = inputs.foldLeft(CountOp.zero)(CountOp.accumulate)
    val result = CountOp.finish(countAcc)
    assert(result == 4L)
  }

  test("AvgOp computes correct average and handles zero count gracefully") {
    val inputs: Seq[Any] = Seq(2, 4L, "6")
    val avgAcc = inputs.foldLeft(AvgOp.zero)(AvgOp.accumulate)
    val result = AvgOp.finish(avgAcc)
    assert(result == 4.0)

    // empty
    val emptyResult = AvgOp.finish(AvgOp.zero)
    assert(emptyResult == 0.0)
  }

  test("AvgOp throws on unsupported input type") {
    assertThrows[IllegalArgumentException] {
      AvgOp.accumulate(AvgOp.zero, 1.5)
    }
  }

  test("UniqueOp accumulates distinct values and finishes with count") {
    val inputs: Seq[Any] = Seq("a", "b", "a", 1, 1)
    val uniqAcc = inputs.foldLeft(UniqueOp.zero)(UniqueOp.accumulate)
    val result = UniqueOp.finish(uniqAcc)
    assert(result == 3) // "a","b",1
  }

  test("UniqueOp zero is empty set and finish returns 0") {
    val emptyResult = UniqueOp.finish(UniqueOp.zero)
    assert(emptyResult == 0)
  }
}
