package fr.xebia.streams.video

import akka.NotUsed
import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream._
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.{ ByteString, Timeout }
import fr.xebia.streams.RemoteWebcamWindow._
import fr.xebia.streams.transform.ByteStringUtils
import fr.xebia.streams.transform.ByteStringUtils._
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
      val httpRequest = HttpRequest(uri = s"http://$host/html/cam_pic_new.php")

      val eventualChunks: Future[Source[ByteString, Any]] = Http()
        .singleRequest(httpRequest)
        .map { response => logger.warn(response.toString()); response }
        .map(_.entity.dataBytes)

      eventualChunks
        .map(splitIntoFrames)
    }

    def splitIntoFrames(source: Source[ByteString, Any]): Source[ByteString, Any] = {
      source
        .via(new FrameChunker(beginOfFrame, endOfFrame))
        .map { bytes => logger.info(bytes.toString()); bytes }
    }

  }

}

import akka.stream.stage._

class FrameChunker(val beginOfFrame: ByteString, val endOfFrame: ByteString) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in = Inlet[ByteString]("Chunker.in")
  val out = Outlet[ByteString]("Chunker.out")
  override val shape = FlowShape.of(in, out)

  val logger = LoggerFactory.getLogger(getClass)

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
        val slicePosition = buffer.indexOfSlice(endOfFrame.toList)
        if (slicePosition >= 0) {
          val (rawChunk, rawNextBuffer) = buffer.splitAt(slicePosition)

          val chunk = trimChunk(rawChunk, beginOfFrame.toList)
          val nextBuffer = trimChunk(rawNextBuffer, endOfFrame.toList)

          logger.info("beginOfFrame ==== " + beginOfFrame.map(_.toString))
          logger.info("endOfFrame ==== " + endOfFrame.map(_.toString))
          logger.info(s"Chunk(${chunk.size}) with head ==== " + chunk.slice(0, 2).map(_.toString))
          logger.info(s"Chunk(${chunk.size}) with tail ==== " + chunk.lastOption.toString)
          logger.info(s"Raw next buffer(${rawNextBuffer.size}) with head ==== " + rawNextBuffer.slice(0, 2).map(_.toString))
          logger.info(s"Next buffer(${nextBuffer.size}) with head ==== " + nextBuffer.slice(0, 2).map(_.toString))

          buffer = nextBuffer
          push(out, chunk ++ endOfFrame)
        } else {
          pull(in)
        }
      }
    }

  }
}
