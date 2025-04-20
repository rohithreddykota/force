// src/main/scala/com/rrr/force/domain/PartialResult.scala
package com.rrr.force.domain

/**
 * Represents the result of executing a SubqueryPlan on a specific partition.
 *
 * @param partitionId The partition identifier on which this result was computed.
 * @param data        A sequence of rows, each represented as a map from field name to value.
 */
case class PartialResult(
                                partitionId: Int,
                                data: Seq[Map[String, Any]]
                              )

/**
 * Represents the final aggregated result across all partitions.
 *
 * @param data A sequence of aggregated rows (field name to value).
 */
final case class FinalResult(
                              data: Seq[Map[String, Any]]
                            )
