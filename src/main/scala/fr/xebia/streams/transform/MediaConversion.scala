package fr.xebia.streams.transform

import java.util.function.Supplier

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv.{ Frame, OpenCVFrameConverter }

/**
 * Holds conversion and transformation methods for media types
 */
object MediaConversion {

  // Each thread gets its own greyMat for safety
  private val frameToMatConverter = ThreadLocal.withInitial(new Supplier[OpenCVFrameConverter.ToMat] {
    def get(): OpenCVFrameConverter.ToMat = new OpenCVFrameConverter.ToMat
  })

  private val bytesToMatConverter = ThreadLocal.withInitial(new Supplier[OpenCVFrameConverter.ToIplImage] {
    def get(): OpenCVFrameConverter.ToIplImage = new OpenCVFrameConverter.ToIplImage
  })

  def frameToMat(frame: Frame): Mat = frameToMatConverter.get().convert(frame)

  def matToFrame(mat: Mat): Frame = frameToMatConverter.get().convert(mat)

  def matToIplImage(mat: Mat): IplImage = bytesToMatConverter.get().convert(matToFrame(mat))

  def iplImageToFrame(image: IplImage): Frame = bytesToMatConverter.get().convert(image)

  def iplImageToMat(image: IplImage): Mat = (iplImageToFrame _ andThen frameToMat)(image)

}