package fr.xebia.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import fr.xebia.streams.common.Dimensions
import fr.xebia.streams.transform.{ Flip, MediaConversion }
import fr.xebia.streams.video.Webcam
import org.bytedeco.javacv.CanvasFrame

object LocalWebcamWindow extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val canvas = new CanvasFrame("Webcam")
  //  //Set Canvas frame to close on exit
  canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)

  val imageDimensions = Dimensions(width = 640, height = 480)
  val localCameraSource = Webcam.local(deviceId = 0, dimensions = imageDimensions)

  val graph = localCameraSource
    .map(MediaConversion.toMat) // most OpenCV manipulations require a Matrix
    .map(Flip.horizontal)
    .map(MediaConversion.toFrame) // convert back to a frame
    .map(canvas.showImage)
    .to(Sink.ignore)

  graph.run()

}