package fr.xebia.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import fr.xebia.streams.common.ConfigReader
import fr.xebia.streams.transform.{Flip, MediaConversion}
import fr.xebia.streams.video.ImageProcessingSinks.ShowImageSink
import fr.xebia.streams.video.{RPiCamWebInterface, Webcam}
import org.bytedeco.javacv.CanvasFrame
import org.slf4j.LoggerFactory

object RemoteWebcamWindow extends App {

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
        .map(MediaConversion.matToFrame)
        .to(ShowImageSink(canvas))
    )

  graph.map(_.run())

}