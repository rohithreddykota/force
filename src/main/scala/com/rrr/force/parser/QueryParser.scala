// src/main/scala/com/rrr/force/parser/QueryParser.scala
package com.rrr.force.parser

import com.rrr.force.domain._
import io.circe._
import io.circe.parser       // for parser.parse
import io.circe.generic.semiauto._

object QueryParser {
  // --- Decoders for Filter types ---
  implicit val eqDecoder: Decoder[Filter.Eq]     = deriveDecoder
  implicit val rangeDecoder: Decoder[Filter.Range] = deriveDecoder
  implicit val filterDecoder: Decoder[Filter] = Decoder.instance { c =>
    c.get[String]("type").flatMap {
      case "Eq"    => eqDecoder(c)
      case "Range" => rangeDecoder(c)
      case other   => Left(DecodingFailure(s"Unknown filter type: $other", c.history))
    }
  }

  // --- Decoder for GroupByKey from string ---
  implicit val groupByKeyDecoder: Decoder[GroupByKey] = Decoder.decodeString.emap {
    case "ByEventType" => Right(GroupByKey.ByEventType)
    case "ByOrg"       => Right(GroupByKey.ByOrg)
    case "ByRepo"      => Right(GroupByKey.ByRepo)
    case "ByUser"      => Right(GroupByKey.ByUser)
    case other         => Left(s"Unknown GroupByKey: $other")
  }

  // --- Decoder for AggOp from string ---
  implicit val aggOpDecoder: Decoder[AggOp] = Decoder.decodeString.emap {
    case "SumOp"    => Right(AggOp.SumOp)
    case "CountOp"  => Right(AggOp.CountOp)
    case "AvgOp"    => Right(AggOp.AvgOp)
    case "UniqueOp" => Right(AggOp.UniqueOp)
    case other      => Left(s"Unknown AggOp: $other")
  }

  // --- Custom QueryAST decoder ---
  implicit val queryASTDecoder: Decoder[QueryAST] = Decoder.instance { cursor =>
    for {
      filters      <- cursor.downField("filters").as[Option[Seq[Filter]]]
      groupBy      <- cursor.downField("groupBy").as[Option[Seq[GroupByKey]]]
      aggregations <- cursor.downField("aggregations").as[Option[Seq[(String, AggOp)]]]
    } yield QueryAST(
      filters.getOrElse(Seq.empty),
      groupBy.getOrElse(Seq.empty),
      aggregations.getOrElse(Seq.empty)
    )
  }

  /**
   * Parses a JSON string into a QueryAST.
   *
   * @param jsonStr JSON representation of the query
   * @return Either an error message or the decoded QueryAST
   */
  def parseQuery(jsonStr: String): Either[String, QueryAST] = {
    // 1. Parse the raw JSON string into a Circe Json
    val jsonOrErr: Either[ParsingFailure, Json] = parser.parse(jsonStr)

    // 2. Decode the Json into our QueryAST
    val astOrErr: Either[Error, QueryAST] = jsonOrErr.flatMap(json => json.as[QueryAST])

    // 3. Map Circe errors into plain String messages
    astOrErr.left.map {
      case pf: ParsingFailure  => s"Invalid JSON: ${pf.message}"
      case df: DecodingFailure => s"Decoding error at ${df.history.map(_.toString).mkString(" -> ")}: ${df.message}"
      case other               => s"Unexpected error: ${other.getMessage}"
    }
  }
}
