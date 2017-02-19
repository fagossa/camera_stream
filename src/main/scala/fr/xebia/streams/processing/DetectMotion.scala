package fr.xebia.streams.processing

import akka.stream.scaladsl.Flow
import fr.xebia.streams.transform.MediaConversion
import fr.xebia.streams.transform.MediaConversion.matToIplImage
import org.bytedeco.javacpp.opencv_core.{CvSize, IplImage, Mat, cvAbsDiff}

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
          val diff: IplImage = second.clone()
          cvAbsDiff(first, second, diff)
          diff
      }
      .map(MediaConversion.iplImageToMat)
  }

  def sizeOf(mat: Mat): CvSize = new CvSize(mat.size().width, mat.size().height)

}
