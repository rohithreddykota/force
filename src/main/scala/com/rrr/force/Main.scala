// src/main/scala/com/rrr/force/Main.scala
package com.rrr.force

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import com.rrr.force.actors.Messages.ExecuteSubquery
import com.rrr.force.actors._
import com.rrr.force.utils.DefaultConfigParser
import com.rrr.force.security.DefaultACLService
import com.rrr.force.monitoring.ConsoleMonitoring
import com.rrr.force.storage.DataPartition

object Main extends App {
  /**
   * Key under which all WorkerActor ExecuteSubquery handlers register.
   */
  val WorkerKey: ServiceKey[ExecuteSubquery] =
    ServiceKey[ExecuteSubquery]("worker-service")

  // Top‐level actor system
  val system: ActorSystem[Unit] = ActorSystem(
    Behaviors.setup[Unit] { ctx =>
      // 1. PartitionManager
      val pm = ctx.spawn(PartitionManagerActor(), "partitionManager")
      // 2. BroadcastManager
      val bm = ctx.spawn(BroadcastManagerActor(), "broadcastManager")

      // 3. Load partition IDs from config
      val cfg        = DefaultConfigParser.config
      val partInts   = cfg.getIntList("force.partitions")
      val partitions = partInts.toArray.toSeq.collect { case i: java.lang.Integer => i.toInt }

      // 4. Spawn one WorkerActor per partition and register
      partitions.foreach { pid =>
        val dp        = DataPartition.load("/data/path", pid)
        val workerRef = ctx.spawn(WorkerActor(dp, ConsoleMonitoring), s"worker-$pid")
        // register with receptionist
        ctx.system.receptionist ! Receptionist.register(WorkerKey, workerRef)
      }

      // 5. Create a group router that automatically discovers all workers via WorkerKey
      val routerRef = ctx.spawn(
        Routers.group[ExecuteSubquery](WorkerKey),
        "worker-router"
      )

      // 6. Coordinator
      ctx.spawn(
        CoordinatorActor(
          pm,
          bm,
          routerRef,
          DefaultACLService,
          ConsoleMonitoring
        ),
        "coordinator"
      )

      Behaviors.empty[Unit]
    },
    "DistribuQuerySystem"
  )

  // Ensure clean shutdown
  sys.addShutdownHook {
    system.terminate()
    println("DistribuQuerySystem shutting down...")
  }
}
