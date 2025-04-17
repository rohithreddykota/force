// src/test/scala/com/rrr/force/security/ACLServiceSpec.scala
package com.rrr.force.security

import com.rrr.force.domain.{AggOp, Filter, GroupByKey, QueryAST}
import org.scalatest.funsuite.AnyFunSuite

class ACLServiceSpec extends AnyFunSuite {
  test("DefaultACLService allows any query") {
    val ast = QueryAST(
      filters = Seq(Filter.Eq("eventType", "PushEvent")),
      groupBy = Seq(GroupByKey.ByEventType),
      aggregations = Seq("cnt" -> AggOp.CountOp)
    )
    assert(DefaultACLService.authorize(ast).isRight)
  }
}
