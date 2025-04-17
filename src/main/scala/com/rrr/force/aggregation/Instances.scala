package com.rrr.force.aggregation

/**
 * Default implementations of AggregationOp for common operations.
 */
object Instances {
  // Sum of numeric inputs (assumes inputs are Int or Long)
  implicit object SumOpLong extends AggregationOp[Long] {
    override val zero: Long = 0L

    override def accumulate(acc: Long, input: Any): Long = input match {
      case i: Int => acc + i
      case l: Long => acc + l
      case s: String => acc + s.toLong
      case other => throw new IllegalArgumentException(s"SumOp: unsupported input type ${other.getClass}")
    }

    override def finish(acc: Long): Any = acc
  }

  // Count records (counts each input as 1)
  implicit object CountOp extends AggregationOp[Long] {
    override val zero: Long = 0L

    override def accumulate(acc: Long, input: Any): Long = acc + 1L

    override def finish(acc: Long): Any = acc
  }

  // Average of numeric inputs: accumulate as (sum, count)
  implicit object AvgOp extends AggregationOp[(Long, Long)] {
    override val zero: (Long, Long) = (0L, 0L)

    override def accumulate(acc: (Long, Long), input: Any): (Long, Long) = {
      val (sum, cnt) = acc
      val value = input match {
        case i: Int => i.toLong
        case l: Long => l
        case s: String => s.toLong
        case other => throw new IllegalArgumentException(s"AvgOp: unsupported input type ${other.getClass}")
      }
      (sum + value, cnt + 1L)
    }

    override def finish(acc: (Long, Long)): Any = acc match {
      case (sum, cnt) if cnt > 0 => sum.toDouble / cnt
      case (_, _) => 0.0
    }
  }

  // Unique count: track a Set[Any]
  implicit object UniqueOp extends AggregationOp[Set[Any]] {
    override val zero: Set[Any] = Set.empty

    override def accumulate(acc: Set[Any], input: Any): Set[Any] = acc + input

    override def finish(acc: Set[Any]): Any = acc.size
  }
}
