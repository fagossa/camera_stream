package fr.xebia.streams.video

import akka.actor.{ ActorLogging, ActorSystem, DeadLetterSuppression, Props }
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }
import akka.stream.scaladsl.Source
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv.FrameGrabber.ImageMode
import org.bytedeco.javacv.{ Frame, FrameGrabber }

object Webcam {

  object local {

    /**
     * Builds a Frame [[Source]]
     *
     * @param deviceId device ID for the webcam
     * @param dimensions
     * @param bitsPerPixel
     * @param imageMode
     * @param system ActorSystem
     * @return a Source of [[Frame]]s
     */
    def apply(
      deviceId: Int,
      dimensions: Dimensions,
      bitsPerPixel: Int = CV_8U,
      imageMode: ImageMode = ImageMode.COLOR
    )(implicit system: ActorSystem) = {
      val props = Props(
        new WebcamFramePublisher(
          deviceId = deviceId,
          imageWidth = dimensions.width,
          imageHeight = dimensions.height,
          bitsPerPixel = bitsPerPixel,
          imageMode = imageMode
        )
      )
      val webcamActorRef = system.actorOf(props)
      val webcamActorPublisher = ActorPublisher[Frame](webcamActorRef)

      Source.fromPublisher(webcamActorPublisher)
    }
  }

  object remote {

  }

}
