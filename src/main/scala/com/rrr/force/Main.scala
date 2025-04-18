// src/main/scala/com/rrr/force/Main.scala
package com.rrr.force

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import com.rrr.force.actors.Messages.ExecuteSubquery
import com.rrr.force.actors._
import com.rrr.force.monitoring.ConsoleMonitoring
import com.rrr.force.security.DefaultACLService
import com.rrr.force.storage.DataPartition
import com.rrr.force.utils.DefaultConfigParser

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

object Main {

  /** ServiceKey under which WorkerActors register for ExecuteSubquery. */
  val WorkerKey: ServiceKey[ExecuteSubquery] =
    ServiceKey[ExecuteSubquery]("worker-service")

  def main(args: Array[String]): Unit = {
    // 1. Bootstrap the ActorSystem with an empty behavior that spawns all children.
    val system: ActorSystem[Unit] = ActorSystem(
      Behaviors.setup[Unit] { ctx =>
        val log = ctx.log
        log.info("📡 Starting DistribuQuerySystem…")

        // --- Safe config lookup ---
        val (partitions, dataDir) = try {
          val cfg = DefaultConfigParser.config
          val partList = cfg.getIntList("force.partitions").asScala.map(_.toInt)
          val dir = cfg.getString("force.data-dir")
          log.info(s"Configured partitions = ${partList.mkString(", ")}")
          log.info(s"Data directory        = $dir")
          (partList, dir)
        } catch {
          case ex: Exception =>
            log.error("❌ Failed to read force.partitions or force.data-dir; defaulting to empty.", ex)
            (Seq.empty[Int], "")
        }

        // --- Spawn PartitionManager ---
        val pmRef = ctx.spawn(PartitionManagerActor(), "partitionManager")
        log.info("✅ partitionManager started")

        // --- Spawn BroadcastManager ---
        val bmRef = ctx.spawn(BroadcastManagerActor(), "broadcastManager")
        log.info("✅ broadcastManager started")

        // --- Spawn WorkerActors and register them ---
        partitions.foreach { pid =>
          try {
            val dp = DataPartition.load(dataDir, pid)
            val workerRef = ctx.spawn(WorkerActor(dp, ConsoleMonitoring), s"worker-$pid")
            ctx.system.receptionist ! Receptionist.register(WorkerKey, workerRef)
            log.info(s"✅ worker-$pid loaded & registered")
          } catch {
            case ex: Exception =>
              log.error(s"❌ Failed to load partition $pid from $dataDir", ex)
          }
        }

        // --- Create a group router for workers ---
        val routerRef = ctx.spawn(Routers.group[ExecuteSubquery](WorkerKey), "worker-router")
        log.info("✅ worker-router started")

        // --- Spawn CoordinatorActor ---
        ctx.spawn(
          CoordinatorActor(
            pm = pmRef,
            bm = bmRef,
            workerRouter = routerRef,
            acl = DefaultACLService,
            mon = ConsoleMonitoring
          ),
          "coordinator"
        )
        log.info("✅ coordinator started")

        // Keep this behavior alive
        Behaviors.empty[Unit]
      },
      "DistribuQuerySystem"
    )

    // 2. Install JVM shutdown hook for graceful termination
    sys.addShutdownHook {
      println("🛑 Shutdown signal received, initiating coordinated shutdown...")
      CoordinatedShutdown(system).run(CoordinatedShutdown.UnknownReason)
      // wait up to 30s for cleanup
      Await.result(system.whenTerminated, 30.seconds)
      println("✅ DistribuQuerySystem has terminated gracefully.")
    }

    // 3. Block main thread until ActorSystem terminates
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
