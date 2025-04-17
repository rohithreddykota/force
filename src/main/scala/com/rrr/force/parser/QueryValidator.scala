// src/main/scala/com/rrr/force/parser/QueryValidator.scala
package com.rrr.force.parser

import com.rrr.force.domain.QueryAST

/**
 * Validates business rules on a decoded QueryAST.
 */
object QueryValidator {
  /**
   * Represents a domain validation error.
   */
  sealed trait QueryValidationError {
    def message: String
  }

  case class MissingAggregations(field: String = "aggregations") extends QueryValidationError {
    val message: String = "At least one aggregation must be specified"
  }

  /**
   * Runs validation on a QueryAST. Returns Right(ast) if valid,
   * or Left(QueryValidationError) if any rule is violated.
   */
  def validate(ast: QueryAST): Either[QueryValidationError, QueryAST] = {
    if (ast.aggregations.nonEmpty) Right(ast)
    else Left(MissingAggregations())
  }
}
