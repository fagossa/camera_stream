package fr.xebia.streams.video

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber.ImageMode

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
      val webcamActorPublisher = ActorPublisher[Frame](webcamActorRef)

      Source.fromPublisher(webcamActorPublisher)
    }
  }

  object remote {

    def apply(host: String, port: String)
             (implicit system: ActorSystem): Source[Frame, NotUsed] = {
      Source.fromPublisher(???)
    }

  }

}
