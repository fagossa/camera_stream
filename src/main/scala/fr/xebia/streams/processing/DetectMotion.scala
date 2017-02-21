package fr.xebia.streams.processing

import akka.stream.scaladsl.Flow
import fr.xebia.streams.transform.MediaConversion
import fr.xebia.streams.transform.MediaConversion.matToIplImage
import org.bytedeco.javacpp.opencv_core.{IplImage, Mat}

object DetectMotion {

  def apply(): Flow[Seq[Mat], Mat, _] = {
    Flow[Seq[Mat]]
      .collect { case l if l.size == 2 => (l.head, l.last) }
      .map {
        case (firstMat, secondMat) =>
          val first = matToIplImage(firstMat)
          val second = matToIplImage(secondMat)
          (first, second)
      }
      .map {
        case (first, second) =>
          import org.bytedeco.javacpp.opencv_core.{IplImage, cvAbsDiff}
          val firstSmoothed = smooth(first)
          val secondSmoothed = smooth(second)

          val diff: IplImage = second.clone()
          cvAbsDiff(firstSmoothed, secondSmoothed, diff)
          diff
      }
      .map { diff =>
        import org.bytedeco.javacpp.opencv_imgproc.{CV_THRESH_BINARY, cvThreshold}
        cvThreshold(diff, diff, 64, 255, CV_THRESH_BINARY)
        diff
      }
      .map(MediaConversion.iplImageToMat)
  }

  private def smooth(baseImage: IplImage): IplImage = {
    import org.bytedeco.javacpp.opencv_imgproc.{CV_GAUSSIAN, cvSmooth}
    val newImage: IplImage = baseImage.clone()
    cvSmooth(baseImage, newImage, CV_GAUSSIAN, 9, 9, 1, 1)
    newImage
  }


}
