package fr.xebia.streams.video

import akka.Done
import akka.stream.scaladsl.Sink
import org.bytedeco.javacv.{CanvasFrame, Frame}

import scala.concurrent.Future

object ImageProcessingSinks {

  object ShowImageSink {

    def apply(canvas: CanvasFrame): Sink[Frame, Future[Done]] = {
      Sink.foreach(canvas.showImage(_))
    }

  }

}
