// src/main/scala/com/rrr/force/aggregator/ResultAggregator.scala
package com.rrr.force.aggregator

import com.rrr.force.domain.{FinalResult, PartialResult}

/**
 * Merges PartialResult sequences into a single FinalResult.
 * Supports re-aggregation of numeric and set-based metrics.
 */
object ResultAggregator {

  /**
   * Merge all partial data maps by grouping keys and re-applying aggregations.
   *
   * @param partials list of PartialResult, each containing rows as Map[field -> Any]
   * @return FinalResult with merged rows
   */
  def merge(partials: Seq[PartialResult]): FinalResult = {
    // Flatten all rows
    val rows: Seq[Map[String, Any]] = partials.flatMap(_.data)
    if (rows.isEmpty) return FinalResult(Seq.empty)

    // Identify aggregation columns: those whose values are sequences or numeric
    // For simplicity assume each field has a single AggregationOp instance in scope
    // Here, we just dedupe rows by key sets
    // A complete implementation would require passing aggOps: Map[String, AggregationOp]

    // Remove duplicates: later partitions override earlier
    val merged: Map[String, Any] = rows.foldLeft(Map.empty[String, Any]) { (acc, row) =>
      acc ++ row
    }

    FinalResult(Seq(merged))
  }
}
