package fr.xebia.streams.examples

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object MapConcatSource extends App {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val system = ActorSystem("MySystem")

  implicit val mat = ActorMaterializer()

  val data = ('a' to 'z').toList
    .zipWithIndex
    .map { case (value, index) => value.toString * (index + 1) }
    .map(_.toCharArray)
    .map(_.toList)

  Source(data)
    .mapConcat(identity) // comment this line to the see result
    .runForeach(e => logger.info(e.toString))

  Await.result(system.terminate(), 5.seconds)

}
