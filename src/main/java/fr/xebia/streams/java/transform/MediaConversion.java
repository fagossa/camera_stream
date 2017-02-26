package fr.xebia.streams.java.transform;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.util.function.Supplier;

public class MediaConversion {

    public  static opencv_core.Mat toMat(Frame frame){
        OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
        return toMat.convert(frame);
    }

    public static Frame toFrame(opencv_core.Mat mat){
        OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
        return toMat.convert(mat);
    }

    public static opencv_core.IplImage matToIplImage(opencv_core.Mat mat){
         OpenCVFrameConverter.ToIplImage toIplImage = new OpenCVFrameConverter.ToIplImage();
         return toIplImage.convert(toFrame(mat));
    }

    public static Frame iplImageToFrame(opencv_core.IplImage iplImage){
         OpenCVFrameConverter.ToIplImage toIplImage = new OpenCVFrameConverter.ToIplImage();
         return toIplImage.convert(iplImage);
    }

    public static opencv_core.Mat iplImageToMat(opencv_core.IplImage image){
        return toMat(iplImageToFrame(image));
    } //(iplImageToFrame _ andThen frameToMat)(image)*/

/*
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

    def iplImageToMat(image: IplImage): Mat = (iplImageToFrame _ andThen frameToMat)(image)*/
}
