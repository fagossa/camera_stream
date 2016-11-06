package fr.xebia.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import fr.xebia.streams.transform.{ Flip, MediaConversion }
import fr.xebia.streams.video.{ SourceOps, Webcam }
import org.bytedeco.javacv.CanvasFrame
import org.slf4j.LoggerFactory

object RemoteWebcamWindow extends App {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val canvas = new CanvasFrame("Webcam")
  canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)

  implicit val ec = system.dispatcher

  val remoteCameraSource = Webcam.remote("192.168.0.17")

  val graph = remoteCameraSource
    .map(
      _.via(SourceOps.toMat)
        .map(Flip.horizontal)
        .map(MediaConversion.toFrame)
        .map(canvas.showImage)
        .to(Sink.ignore)
    )

  graph.map(_.run())

}