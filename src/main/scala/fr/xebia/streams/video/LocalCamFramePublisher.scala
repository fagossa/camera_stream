package fr.xebia.streams.video

import akka.actor.{ ActorLogging, DeadLetterSuppression, Props }
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }
import fr.xebia.streams.video.LocalCamFramePublisher.{ Continue, buildGrabber }
import org.bytedeco.javacv.FrameGrabber.ImageMode
import org.bytedeco.javacv.{ Frame, FrameGrabber }

/**
 * Actor that backs the Akka Stream source
 */
private[video] class LocalCamFramePublisher(
    deviceId: Int,
    imageWidth: Int,
    imageHeight: Int,
    bitsPerPixel: Int,
    imageMode: ImageMode
) extends ActorPublisher[Frame] with ActorLogging {

  private implicit val ec = context.dispatcher

  // Lazy so that nothing happens until the flow begins
  private lazy val grabber: FrameGrabber = buildGrabber(
    deviceId = deviceId,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    bitsPerPixel = bitsPerPixel,
    imageMode = imageMode
  )

  def receive: Receive = {
    case _: Request => emitFrames()
    case Continue => emitFrames()
    case Cancel => onCompleteThenStop()
    case unexpectedMsg => log.warning(s"Unexpected message: $unexpectedMsg")
  }

  private def emitFrames(): Unit = {
    if (isActive && totalDemand > 0) {
      /*
        Grabbing a frame is a blocking I/O operation, so we don't send too many at once.
       */
      grabFrame().foreach(onNext)
      if (totalDemand > 0) {
        self ! Continue
      }
    }
  }

  private def grabFrame(): Option[Frame] = {
    Option(grabber.grab())
  }
}

object LocalCamFramePublisher {

  def props(deviceId: Int, width: Int, height: Int, bitsPerPixel: Int, imageMode: ImageMode): Props =
    Props(
      new LocalCamFramePublisher(
        deviceId = deviceId,
        imageWidth = width,
        imageHeight = height,
        bitsPerPixel = bitsPerPixel,
        imageMode = imageMode
      )
    )

  private case object Continue extends DeadLetterSuppression

  // Building a started grabber seems finicky if not synchronised; there may be some freaky stuff happening somewhere.
  private[video] def buildGrabber(
    deviceId: Int,
    imageWidth: Int,
    imageHeight: Int,
    bitsPerPixel: Int,
    imageMode: ImageMode
  ): FrameGrabber = synchronized {
    val g = FrameGrabber.createDefault(deviceId)
    g.setImageWidth(imageWidth)
    g.setImageHeight(imageHeight)
    g.setBitsPerPixel(bitsPerPixel)
    g.setImageMode(imageMode)
    g.start()
    g
  }
}

