package fr.xebia.streams

import java.io.{ BufferedInputStream, ByteArrayOutputStream, FileInputStream, InputStream }

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.bytedeco.javacpp.opencv_core.{ CvSize, IplImage }
import org.bytedeco.javacpp.{ BytePointer, opencv_core }
import org.bytedeco.javacv.CanvasFrame
import org.scalatest._

class ByteStringToMatSpec extends FlatSpec with MustMatchers {

  def readBinaryFile(input: InputStream): Array[Byte] = {
    val fos = new ByteArrayOutputStream(65535)
    val bis = new BufferedInputStream(input)
    val buf = new Array[Byte](1024)
    Stream.continually(bis.read(buf))
      .takeWhile(_ != -1)
      .foreach(fos.write(buf, 0, _))
    fos.toByteArray
  }

  "ByteStringMapper" should "transform byte array to image" in {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    // Given an image
    val data: Array[Byte] = readBinaryFile(new FileInputStream("src/test/resources/content.data"))

    // Given a size
    val width = 512
    val height = 288

    // When an image is loaded
    val imageData: IplImage = opencv_core.cvCreateImage(new CvSize(width, height), opencv_core.CV_8UC3, 3)
    imageData.imageData(new BytePointer(data: _*))

    val canvas = new CanvasFrame("Webcam")
    //canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

    // When the image is shown
    import org.bytedeco.javacv.OpenCVFrameConverter
    val converter = new OpenCVFrameConverter.ToIplImage
    canvas.showImage(converter.convert(imageData))

    // Then
    imageData.width() mustBe width
    imageData.height() mustBe height
  }

}
