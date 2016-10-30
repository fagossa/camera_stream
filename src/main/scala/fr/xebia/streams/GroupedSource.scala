package fr.xebia.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object GroupedSource extends App {
  val logger = LoggerFactory.getLogger(getClass)

  implicit val system = ActorSystem("MySystem")

  implicit val mat = ActorMaterializer()

  val data = 1 to 99

  Source(data)
    .grouped(10)
    .runWith(Sink.foreach(e => logger.info(e.toString)))

  Await.result(system.terminate(), 5.seconds)
}
