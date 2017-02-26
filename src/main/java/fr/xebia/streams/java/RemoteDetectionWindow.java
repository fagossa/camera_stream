package fr.xebia.streams.java;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import fr.xebia.streams.java.common.Dimensions;
import fr.xebia.streams.java.transform.MediaConversion;
import fr.xebia.streams.java.video.Camera;
import fr.xebia.streams.java.video.RemoteProvider;
import fr.xebia.streams.transform.Flip;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.bytedeco.javacpp.opencv_core.CV_8U;

public class RemoteDetectionWindow {

    public void init() throws ExecutionException, InterruptedException {
        CanvasFrame webcam = new CanvasFrame("Webcam");
        webcam.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        Dimensions imageDimensions = new Dimensions(640, 480);
        ActorSystem actorSystem = ActorSystem.create();
        ActorMaterializer materializer = ActorMaterializer.create(actorSystem);


        Future<Source<opencv_core.Mat, Object>> remoteCamera = Camera.remoteWebCam(new RemoteProvider(), actorSystem, materializer);


        remoteCamera.get()
                .map(Flip::horizontal)
                .grouped(2)
                .via(fr.xebia.streams.java.processing.DetectMotion.build())
                .map(MediaConversion::toFrame) // convert back to a frame
                .map((image) -> {
                    webcam.showImage(image);
                    return image;
                })
                .to(Sink.ignore())
                .run(materializer);
    }

}
