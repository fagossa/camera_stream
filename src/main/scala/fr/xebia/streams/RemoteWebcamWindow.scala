package fr.xebia.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString
import fr.xebia.streams.transform.MediaConversion
import fr.xebia.streams.video.Webcam
import org.bytedeco.javacpp.opencv_core
import org.bytedeco.javacpp.opencv_core.Mat
import org.bytedeco.javacv.CanvasFrame
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

object RemoteWebcamWindow extends App {

  val logger = LoggerFactory.getLogger(getClass)

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val canvas = new CanvasFrame("Webcam")
  canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)

  implicit val ec = system.dispatcher

  val remoteCameraSource = Webcam.remote("192.168.0.17")

  val graph = remoteCameraSource
    .map(handleSource)
    .map(_.map(MediaConversion.toFrame))
    .map(_.map(canvas.showImage))
    .map(_.to(Sink.ignore))

  graph.map(_.run())

  def handleSource(source: Source[ByteString, Any])(implicit ec: ExecutionContext) = {
    source
      .map { bytes => logger.info(bytes.toString()); bytes }
      .map(_.toArray)
      .map { bytes =>
        val mat = new Mat(512, 288, opencv_core.CV_8SC3)
        mat.data().put(bytes: _*)
        mat
      }
  }

}