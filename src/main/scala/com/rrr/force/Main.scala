// src/main/scala/com/rrr/force/Main.scala
package com.rrr.force

import java.nio.file.Paths
import com.rrr.force.actors.Messages.{QueryRequest, QueryResponse}
import akka.actor.typed.ActorRef

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.{Balance, FileIO, GraphDSL, JsonFraming, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, Materializer}
import akka.util.ByteString
import io.circe.parser.decode
import com.rrr.force.actors.Messages.{ExecuteSubquery, SubqueryResult}
import com.rrr.force.actors.{BroadcastManagerActor, CoordinatorActor, PartitionManagerActor, WorkerActor}
import com.rrr.force.domain.{GitHubDecoders, GitHubEvent, SubqueryPlan}
import com.rrr.force.monitoring.ConsoleMonitoring
import com.rrr.force.security.DefaultACLService
import com.rrr.force.utils.{DefaultConfigParser, MyPlanner}

import scala.concurrent.duration.DurationInt
import scala.io.StdIn

object Main extends App {
  ActorSystem[Unit](
    Behaviors.setup[Unit] { ctx =>
      val pm = ctx.spawn(PartitionManagerActor(),   "partitionManager")
      val bm = ctx.spawn(BroadcastManagerActor(),   "broadcastManager")
      implicit val classic = ctx.system.toClassic
      implicit val mat     = Materializer(classic)
      implicit val ec      = ctx.system.executionContext

      val cfg       = DefaultConfigParser.config
      val inputPath = cfg.getString("force.input-file")
      val workerCnt = cfg.getInt("force.worker-count")

      // 1. Pool Router
      val router = ctx.spawn(
        Routers.pool[ExecuteSubquery](workerCnt)(
          Behaviors.setup(_ => WorkerActor(ConsoleMonitoring))
        ),
        "worker-pool"
      )

      val coordinator: ActorRef[Any] =
        ctx.spawn(
          CoordinatorActor(workerCnt, router, DefaultACLService, ConsoleMonitoring),
          "coordinator"
        )

      import GitHubDecoders._

      // 3. stream and partition
      val source: Source[GitHubEvent, _] =
        FileIO.fromPath(Paths.get(inputPath))
          .via(JsonFraming.objectScanner(65536))
          .map(_.utf8String)
          .map(json => decode[GitHubEvent](json).fold(err => throw new RuntimeException(err), identity))

      val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._
        val bal = b.add(Balance[GitHubEvent](workerCnt))
        val in  = b.add(source)
        in ~> bal.in

        for (i <- 0 until workerCnt) {
          val sink = Sink.foreach[GitHubEvent] { evt =>
            val logicPlan = MyPlanner.plan(evt)
            println(f"[partition $i%2d] got event id=${evt.id}")
            val subPlan   = SubqueryPlan(logicPlan, evt, i)
            router ! ExecuteSubquery(subPlan, coordinator)
          }
          bal.out(i) ~> b.add(sink).in
        }
        ClosedShape
      })

      graph.run()


      // 4) Console Loop
      Future {
        println("=== DistribuQuery CLI ===")
        println("input JSON query：")
        Iterator
          .continually(StdIn.readLine("> "))
          .takeWhile(line => line != null && line.trim.toLowerCase != "exit")
          .foreach { line =>
            if (line.nonEmpty) {
              implicit val timeout: akka.util.Timeout = 5.seconds

              implicit val scheduler: akka.actor.typed.Scheduler = ctx.system.scheduler

              val replyF: Future[QueryResponse] =
                coordinator.ask(ref => QueryRequest(line, ref))

              replyF.onComplete {
                case scala.util.Success(QueryResponse.Success(finalRes)) =>
                  println("== Query Result ==")
                  println(finalRes)
                case scala.util.Success(QueryResponse.Failure(err)) =>
                  println(s"Query failed: $err")
                case scala.util.Failure(ex) =>
                  println(s"Error asking coordinator: $ex")
              }
            }
          }

        println("CLI exiting, shutting down…")
        ctx.system.terminate()
      }(ec)

      Behaviors.empty
    },
    "DistribuQuerySystem"
  )
}
