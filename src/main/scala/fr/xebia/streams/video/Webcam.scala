package fr.xebia.streams.video

import akka.NotUsed
import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream._
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Flow, Source }
import akka.util.{ ByteString, Timeout }
import fr.xebia.streams.RemoteWebcamWindow._
import fr.xebia.streams.transform.{ Implicits, MediaConversion }
import fr.xebia.streams.video.SourceOps.toDisk
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber.ImageMode

import scala.concurrent.{ ExecutionContext, Future }

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
    implicit val timeout = Timeout(5.seconds)

    val beginOfFrame = ByteString(0xff, 0xd8)

    val endOfFrame = ByteString(0xff, 0xd9)

    def apply(host: String)(implicit system: ActorSystem, mat: Materializer): Future[Source[ByteString, Any]] = {
      implicit val ec = system.dispatcher
      val httpRequest = HttpRequest(uri = s"http://$host/html/cam_pic_new.php")

      val eventualChunks: Future[Source[ByteString, Any]] = Http()
        .singleRequest(httpRequest)
        .map(_.entity.dataBytes)

      eventualChunks
        .map(splitIntoFrames)
    }

    def splitIntoFrames[B](source: Source[ByteString, B]): Source[ByteString, B] =
      source
        .via(new FrameChunker(beginOfFrame, endOfFrame))
        .via(toDisk(s"content.data"))

  }

}

import akka.stream.stage._

class FrameChunker(val beginOfFrame: ByteString, val endOfFrame: ByteString) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in = Inlet[ByteString]("Chunker.in")
  val out = Outlet[ByteString]("Chunker.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var buffer = ByteString.empty

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (isClosed(in)) emitChunk()
        else pull(in)
      }
    })
    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem = grab(in)
        buffer ++= elem
        emitChunk()
      }

      override def onUpstreamFinish(): Unit = {
        if (buffer.isEmpty) completeStage()
        // elements left in buffer, keep accepting downstream pulls
        // and push from buffer until buffer is emitted
      }
    })

    private def emitChunk(): Unit = {
      if (buffer.isEmpty) {
        if (isClosed(in)) completeStage()
        else pull(in)
      } else {
        import Implicits._
        buffer.sliceChunk(beginOfFrame, endOfFrame) match {
          case Some((chunk, remaining)) =>
            buffer = remaining
            push(out, chunk)

          case None =>
            pull(in)
        }
      }
    }

  }
}

object SourceOps {
  import org.bytedeco.javacpp.{ BytePointer, Pointer, opencv_core, opencv_imgcodecs }
  import org.bytedeco.javacpp.opencv_core.{ CvMat, CvSize, Mat, cvMat }
  import org.bytedeco.javacpp.opencv_imgcodecs._

  def toDisk(filename: String): Flow[ByteString, ByteString, _] = {
    Flow[ByteString]
      .map { content =>
        import java.io.FileOutputStream
        import java.io.BufferedOutputStream
        val bos = new BufferedOutputStream(new FileOutputStream(filename))
        bos.write(content.toArray)
        bos.close()
        content
      }
  }

  def toMat(implicit ec: ExecutionContext): Flow[ByteString, Mat, NotUsed] = {
    Flow[ByteString]
      .map(_.toArray)
      .map { bytes =>
        val image = opencv_core.cvCreateImage(new CvSize(512, 288), opencv_core.CV_8UC3, 3)
        image.imageData(new BytePointer(bytes: _*))
        image
      }
      .map(MediaConversion.toFrame)
      .map(MediaConversion.toMat)
      .map(imdecode(_, opencv_imgcodecs.CV_LOAD_IMAGE_COLOR))
  }

}
