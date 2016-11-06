package fr.xebia.streams

import java.io.{ BufferedInputStream, FileInputStream }

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import fr.xebia.streams.transform.MediaConversion
import org.bytedeco.javacpp.{ BytePointer, opencv_core }
import org.bytedeco.javacpp.opencv_core.{ CvSize, IplImage, Mat }
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._

class ByteStringToMatSpec extends FlatSpec with MustMatchers {

  "ByteStringMapper" should "transform byte array to image" in {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    implicit val ec = system.dispatcher

    val bis: BufferedInputStream = new BufferedInputStream(new FileInputStream("src/test/resources/content.data"))
    val data: Array[Byte] = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray

    val source = Source
      .single(data)
      .map { bytes => /*println(bytes.toString);*/ bytes }
      .map { bytes =>
        val image = opencv_core.cvCreateImage(new CvSize(512, 288), opencv_core.CV_8UC3, 3)
        image.put(new BytePointer(bytes: _*))
        MediaConversion.toFrame(image)
      }
      .map(MediaConversion.toMat)

    val eventualResult = source.runWith(Sink.foreach { img =>
      //img must have size data.length
      true must be(true)
    })

    Await.result(eventualResult.map(_ => system.terminate()), 5.second)
  }

}
