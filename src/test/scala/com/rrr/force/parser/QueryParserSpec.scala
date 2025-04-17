// src/test/scala/com/rrr/force/parser/QueryParserSpec.scala
package com.rrr.force.parser

import com.rrr.force.domain._
import org.scalatest.funsuite.AnyFunSuite

class QueryParserSpec extends AnyFunSuite {
  val baseJson =
    """
      |{
      |  "filters": [
      |    { "type": "Eq", "field": "eventType", "value": "PushEvent" },
      |    { "type": "Range", "field": "createdAt", "start": "2025-01-01T00:00:00Z", "end": "2025-12-31T23:59:59Z" }
      |  ],
      |  "groupBy": ["ByEventType", "ByRepo"],
      |  "aggregations": [["size", "SumOp"], ["distinctSize", "AvgOp"], ["id", "CountOp"]]
      |}
    """.stripMargin

  test("parseQuery should succeed on valid JSON") {
    val result = QueryParser.parseQuery(baseJson)
    assert(result.isRight)
    val ast = result.toOption.get
    assert(ast.filters.size == 2)
    assert(ast.groupBy == Seq(GroupByKey.ByEventType, GroupByKey.ByRepo))
    assert(ast.aggregations.map(_._2) == Seq(AggOp.SumOp, AggOp.AvgOp, AggOp.CountOp))
  }

  test("parseQuery should fail on invalid JSON") {
    val invalidJson = "{ this is not valid JSON }"
    val result = QueryParser.parseQuery(invalidJson)
    assert(result.isLeft)
    assert(result.left.get.startsWith("Invalid JSON"))
  }

  test("parseQuery should fail on unknown filter type") {
    val badFilterJson =
      """
        |{ "filters": [{ "type":"Foo", "field":"x", "value":"y" }] }
      """.stripMargin
    val result = QueryParser.parseQuery(badFilterJson)
    assert(result.isLeft)
    assert(result.left.get.contains("Unknown filter type"))
  }

  test("parseQuery should fail on unknown groupBy key") {
    val badGroupByJson =
      """
        |{ "groupBy": ["ByFoo"] }
      """.stripMargin
    val result = QueryParser.parseQuery(badGroupByJson)
    assert(result.isLeft)
    assert(result.left.get.contains("Unknown GroupByKey"))
  }

  test("parseQuery should fail on unknown AggOp") {
    val badAggJson =
      """
        |{ "aggregations": [["size","FooOp"]] }
      """.stripMargin
    val result = QueryParser.parseQuery(badAggJson)
    assert(result.isLeft)
    assert(result.left.get.contains("Unknown AggOp"))
  }
}

class QueryValidatorSpec extends AnyFunSuite {
  test("validate should succeed when aggregations present") {
    val ast = QueryAST(aggregations = Seq("size" -> AggOp.SumOp))
    val result = QueryValidator.validate(ast)
    assert(result == Right(ast))
  }

  test("validate should fail when aggregations empty") {
    val ast = QueryAST()
    val result = QueryValidator.validate(ast)
    assert(result.isLeft)
    assert(result.left.get.message == "At least one aggregation must be specified")
  }
}
