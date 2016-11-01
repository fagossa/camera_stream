package fr.xebia.streams.video

import akka.actor.{ ActorLogging, DeadLetterSuppression, Props }
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }
import akka.stream.scaladsl.Tcp
import fr.xebia.streams.video.RemoteCamFramePublisher.{ Continue, buildGrabber }
import org.bytedeco.javacv.Frame

private[video] class RemoteCamFramePublisher(url: String)
    extends ActorPublisher[Frame] with ActorLogging {

  private implicit val ec = context.dispatcher

  private lazy val grabber = buildGrabber(url)

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

object RemoteCamFramePublisher {

  def props(url: String): Props = Props(new RemoteCamFramePublisher(url))

  def buildGrabber(url: String) = RemoteFrameGrabber(url)

  private case object Continue extends DeadLetterSuppression

  case class RemoteFrameGrabber(url: String) {
    def grab(): Frame = ???
  }

  //Tcp().bindAndHandle()

}