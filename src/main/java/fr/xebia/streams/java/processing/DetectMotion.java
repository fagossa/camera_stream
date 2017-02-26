package fr.xebia.streams.java.processing;

import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import fr.xebia.streams.java.transform.MediaConversion;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.IplImage;

import java.util.List;

import static fr.xebia.streams.java.transform.MediaConversion.matToIplImage;
import static org.bytedeco.javacpp.opencv_core.cvAbsDiff;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class DetectMotion {

    public static Flow<List<opencv_core.Mat>, opencv_core.Mat, NotUsed> build() {
        return Flow.<List<opencv_core.Mat>>create()
                .filter(l -> l.size() == 2)
                .map(l -> new Pair<>(l.get(0), l.get(1)))
                .map(p -> new Pair<>(matToIplImage(p.first()), matToIplImage(p.second())))
                .map(p -> {
                    IplImage smoothFirst = smooth(p.first());
                    IplImage smoothSecond = smooth(p.second());
                    IplImage diff = smoothFirst.clone();
                    cvAbsDiff(smoothFirst, smoothSecond, diff);
                    return diff;
                })
                .map(diff -> {
                    cvThreshold(diff, diff, 64, 255, CV_THRESH_BINARY);
                    return diff;
                })
                .map(MediaConversion::iplImageToMat);
    }

    private static IplImage smooth(IplImage baseImage) {
        IplImage newImage = baseImage.clone();
        cvSmooth(baseImage, newImage, CV_GAUSSIAN, 9, 9, 1, 1);
        return newImage;
    }

}
