// src/main/scala/BalanceTest.scala
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Balance, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, Materializer}

object BalanceTest extends App {
  // 1. 用 Typed ActorSystem 和 Materializer
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "BalanceTest")
  implicit val mat: Materializer             = Materializer(system)
  implicit val ec = system.executionContext

  val workerCnt = 3

  // 2. 构造一个简单的 Source(1 to 10)
  val src = Source(1 to 10).map(n => s"item-$n")

  // 3. 用 GraphDSL 构建一个带 Balance 的流
  val graph: RunnableGraph[_] = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    // Balance 插件，3 个下游
    val bal = b.add(Balance[String](workerCnt))
    val in  = b.add(src)

    // 3.1 上游 → Balance
    in ~> bal.in

    // 3.2 Balance 的每个出口都连一个 Sink.foreach，打印分区号和元素
    for (i <- 0 until workerCnt) {
      val sink = b.add(
        Sink.foreach[String] { elem =>
          println(s"[partition $i] got $elem")
        }
      )
      bal.out(i) ~> sink.in
    }

    ClosedShape
  })

  // 4. 运行流
  graph.run()

  // 5. 等几秒再优雅 shutdown，保证日志都能刷出来
  Thread.sleep(1000)
  system.terminate()
}
