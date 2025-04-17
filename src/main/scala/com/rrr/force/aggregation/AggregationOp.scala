// src/main/scala/com/rrr/force/aggregation/AggregationOp.scala
package com.rrr.force.aggregation

/**
 * Type class for aggregation operations over a sequence of values.
 *
 * @tparam V The intermediate accumulator type.
 */
trait AggregationOp[V] {
  /** The zero (empty) value for this aggregation. */
  def zero: V

  /**
   * Incorporate the next input value into the accumulator.
   *
   * @param acc   Current accumulator
   * @param input Next raw input (Any) from the dataset
   * @return Updated accumulator
   */
  def accumulate(acc: V, input: Any): V

  /**
   * Finalize the aggregation, producing a concrete output value (Any).
   *
   * @param acc Final accumulator
   */
  def finish(acc: V): Any
}