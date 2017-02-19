package fr.xebia.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import fr.xebia.streams.common.ConfigReader
import fr.xebia.streams.processing.DetectMotion
import fr.xebia.streams.transform.{ Flip, MediaConversion }
import fr.xebia.streams.video.{ RPiCamWebInterface, Webcam }
import org.bytedeco.javacv.CanvasFrame
import org.slf4j.LoggerFactory

object RemoteDetectionWindow extends App {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val canvas = new CanvasFrame("Webcam")
  canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)

  implicit val ec = system.dispatcher

  val remoteCameraSource = Webcam.remote(RPiCamWebInterface(ConfigReader.host))

  val graph = remoteCameraSource
    .map(
      _.map(Flip.horizontal)
        .grouped(2)
        .via(DetectMotion())
        .map(MediaConversion.matToFrame)
        .map(canvas.showImage)
        .to(Sink.ignore)
    )

  graph.map(_.run())

}