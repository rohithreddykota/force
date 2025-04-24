// src/main/scala/com/rrr/force/Main.scala
package com.rrr.force

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.util.Timeout
import com.rrr.force.actors.Messages.{ExecuteSubquery, QueryRequest, QueryResponse}
import com.rrr.force.actors._
import com.rrr.force.monitoring.ConsoleMonitoring
import com.rrr.force.security.DefaultACLService
import com.rrr.force.storage.DataPartition
import com.rrr.force.broadcast.BroadcastData
import com.rrr.force.utils.DefaultConfigParser
import kamon.Kamon

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.StdIn

object Main {

  /** ServiceKey under which WorkerActors register for ExecuteSubquery. */
  val WorkerKey: ServiceKey[ExecuteSubquery] =
    ServiceKey[ExecuteSubquery]("worker-service")

  def main(args: Array[String]): Unit = {

    Kamon.init()

    val system: ActorSystem[Unit] = ActorSystem(
      Behaviors.setup[Unit] { ctx =>
        val log = ctx.log
        log.info("📡 Starting DistribuQuerySystem…")

        // safe config lookup
        val (partitions, dataDir) = try {
          val cfg      = DefaultConfigParser.config
          val partList = cfg.getIntList("force.partitions").asScala.map(_.toInt)
          val dir      = cfg.getString("force.data-dir")
          (partList, dir)
        } catch {
          case ex: Exception =>
            log.error("❌ misconfigured force.partitions or force.data-dir", ex)
            (Seq.empty, "")
        }

        // spawn managers
        val pmRef = ctx.spawn(PartitionManagerActor(), "partitionManager")
        val bmRef = ctx.spawn(BroadcastManagerActor(), "broadcastManager")

        // spawn & register workers
        partitions.foreach { pid =>
          val dp = DataPartition.load(dataDir, pid)                             // :contentReference[oaicite:0]{index=0}&#8203;:contentReference[oaicite:1]{index=1}
          val w  = ctx.spawn(WorkerActor(dp, ConsoleMonitoring), s"worker-$pid")
          ctx.system.receptionist ! Receptionist.register(WorkerKey, w)
        }

        // group router
        val router = ctx.spawn(Routers.group[ExecuteSubquery](WorkerKey), "worker-router")

        // spawn coordinator and keep a ref
        val coordinatorRef =
          ctx.spawn(
            CoordinatorActor(pmRef, bmRef, router, DefaultACLService, ConsoleMonitoring),
            "coordinator"
          )

        // ── Interactive CLI loop ─────────────────────────────────────
        implicit val ec        = ctx.system.executionContext
        implicit val sched     = ctx.system.scheduler
        implicit val timeout   = Timeout(5.seconds)

        Future {
          println("=== DistribuQuery CLI ===")
          println("Type JSON query or 'exit':")

          Iterator.continually(StdIn.readLine("> "))
            .takeWhile(line => line != null && line.trim.toLowerCase != "exit")
            .foreach { line =>
              if (line.trim.nonEmpty) {
                // send the raw JSON to the coordinator
                val replyF = coordinatorRef.ask[QueryResponse](ref => QueryRequest(line, ref))
                replyF.onComplete {
                  case scala.util.Success(QueryResponse.Success(fr)) =>
                    println("✅ Result:")
                    println(fr.data.mkString("\n"))

                  case scala.util.Success(QueryResponse.Failure(reason)) =>
                    println(s"✗ Query failed: $reason")

                  case scala.util.Failure(err) =>
                    println(s"‼ Unexpected error: ${err.getMessage}")
                }
              }
            }

          println("🛑 Exiting CLI, shutting down system…")
          ctx.system.terminate()
        }

        Behaviors.empty
      },
      "DistribuQuerySystem"
    )

    // graceful shutdown hook
    sys.addShutdownHook {
      CoordinatedShutdown(system).run(CoordinatedShutdown.UnknownReason)
      Await.result(system.whenTerminated, 30.seconds)
      Kamon.stop()
    }

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
