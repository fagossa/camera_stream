package fr.xebia.streams.video

import akka.NotUsed
import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.Materializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Framing, Source }
import akka.util.{ ByteString, Timeout }
import fr.xebia.streams.RemoteWebcamWindow._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber.ImageMode
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object Webcam {

  object local {

    def apply(
      deviceId: Int,
      dimensions: Dimensions,
      bitsPerPixel: Int = CV_8U,
      imageMode: ImageMode = ImageMode.COLOR
    )(implicit system: ActorSystem): Source[Frame, NotUsed] = {
      val props: Props = LocalCamFramePublisher.props(deviceId, dimensions.width, dimensions.height, bitsPerPixel, imageMode)
      val webcamActorRef = system.actorOf(props)
      val localActorPublisher = ActorPublisher[Frame](webcamActorRef)

      Source.fromPublisher(localActorPublisher)
    }
  }

  object remote {

    import scala.concurrent.duration._
    val logger = LoggerFactory.getLogger(getClass)

    implicit val timeout = Timeout(5.seconds)

    val beginOfFrame = ByteString(0xff, 0xd8)

    val endOfFrame = ByteString(0xff, 0xd9)

    //http://doc.akka.io/docs/akka/2.4/scala/stream/stream-cookbook.html#chunking-up-a-stream-of-bytestrings-into-limited-size-bytestrings
    def apply(host: String)(implicit system: ActorSystem, mat: Materializer): Future[Source[ByteString, Any]] = {
      implicit val ec = system.dispatcher
      val httpRequest = HttpRequest(uri = "/html/cam_pic_new.php")

      Http()
        .singleRequest(httpRequest)
        .map { response => logger.info(response.toString()); response }
        .map(_.entity.dataBytes)
        .map(_.via(Framing.delimiter(endOfFrame, maximumFrameLength = 100, allowTruncation = true)))
        .map(_.map(_.dropWhile(_ != beginOfFrame)))
    }

  }

}
