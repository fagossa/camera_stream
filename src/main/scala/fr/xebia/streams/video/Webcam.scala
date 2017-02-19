package fr.xebia.streams.video

import akka.NotUsed
import akka.actor.{ ActorSystem, Props }
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream._
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Flow, Source }
import akka.util.{ ByteString, Timeout }
import fr.xebia.streams.common.Dimensions
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv.Frame

import scala.concurrent.{ ExecutionContext, Future }

object Webcam {

  object local {

    import org.bytedeco.javacv.FrameGrabber.ImageMode

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

    implicit val timeout = Timeout(5.seconds)

    val beginOfFrame = ByteString(0xff, 0xd8)

    val endOfFrame = ByteString(0xff, 0xd9)

    def apply(provider: RemoteProvider)(implicit system: ActorSystem, mat: Materializer): Future[Source[Mat, Any]] = {
      implicit val ec = system.dispatcher
      val httpRequest = HttpRequest(uri = provider.uri)

      val eventualChunks: Future[Source[ByteString, Any]] =
        Http()
          .singleRequest(httpRequest)
          .map(_.entity.dataBytes)

      // TODO: issue-4 comes from here!
      eventualChunks
        .map(
          _.log("reading logs", identity)
            .via(new FrameChunker(beginOfFrame, endOfFrame)).withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
            .via(bytesToFile(s"content.data"))
            .via(bytesToMat(provider))
        )
    }

    def bytesToFile(filename: String): Flow[ByteString, ByteString, _] = {
      Flow[ByteString]
        .map { content =>
          import java.io.{ BufferedOutputStream, FileOutputStream }
          import fr.xebia.streams.common.IoOps._
          using(new BufferedOutputStream(new FileOutputStream(filename))) { bos =>
            bos.write(content.toArray)
          }
          content
        }
    }

    def bytesToMat(provider: RemoteProvider)(implicit ec: ExecutionContext): Flow[ByteString, Mat, NotUsed] = {
      import fr.xebia.streams.transform.MediaConversion
      import org.bytedeco.javacpp.opencv_core.CvSize
      import org.bytedeco.javacpp.opencv_imgcodecs._
      import org.bytedeco.javacpp.{ BytePointer, opencv_core, opencv_imgcodecs }
      Flow[ByteString]
        .map(_.toArray)
        .map { bytes =>
          val frameSize = new CvSize(provider.width, provider.height)
          val image = opencv_core.cvCreateImage(frameSize, opencv_core.CV_8UC3, 3)
          image.imageData(new BytePointer(bytes: _*))
          image
        }
        .map(MediaConversion.iplImageToFrame)
        .map(MediaConversion.frameToMat)
        .map(imdecode(_, opencv_imgcodecs.CV_LOAD_IMAGE_COLOR))
    }

  }

}
