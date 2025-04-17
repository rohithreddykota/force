// src/main/scala/com/rrr/force/domain/QueryAST.scala
package com.rrr.force.domain

import java.time.Instant

/** Filter operations for query predicates. */
sealed trait Filter
object Filter {
  /** Equals filter: field == value */
  final case class Eq(field: String, value: String) extends Filter

  /** Range filter: start <= field <= end */
  final case class Range(field: String, start: Instant, end: Instant) extends Filter
}

/** Keys available for grouping results. */
sealed trait GroupByKey
object GroupByKey {
  final case object ByEventType extends GroupByKey
  final case object ByOrg       extends GroupByKey
  final case object ByRepo      extends GroupByKey
  final case object ByUser      extends GroupByKey
}

/** Supported aggregation operations. */
sealed trait AggOp
object AggOp {
  final case object SumOp    extends AggOp   // Sum numeric fields
  final case object CountOp  extends AggOp   // Count records
  final case object AvgOp    extends AggOp   // Average numeric fields
  final case object UniqueOp extends AggOp   // Count unique values
}

/**
 * Represents a parsed query:
 *  - `filters`: predicates to apply
 *  - `groupBy`: dimensions to group on
 *  - `aggregations`: field & operation pairs
 */
final case class QueryAST(
                           filters: Seq[Filter]       = Seq.empty,
                           groupBy: Seq[GroupByKey]   = Seq.empty,
                           aggregations: Seq[(String, AggOp)] = Seq.empty
                         )
