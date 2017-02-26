package fr.xebia.streams.java.video;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.stream.Attributes;
import akka.stream.Materializer;
import akka.stream.actor.AbstractActorPublisher;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import fr.xebia.streams.java.common.Dimensions;
import fr.xebia.streams.java.transform.MediaConversion;
import fr.xebia.streams.video.FrameChunker;
import fr.xebia.streams.video.LocalCamFramePublisher;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.reactivestreams.Publisher;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Camera {

    public static WebCam localWebCam = (Integer deviceId,
                                        Dimensions dimensions,
                                        Integer bitsPerPixel,
                                        FrameGrabber.ImageMode imageMode, ActorSystem system) -> {
        Props props = LocalCamFramePublisher.props(deviceId, dimensions.getWidth(), dimensions.getHeight(), bitsPerPixel, imageMode);
        ActorRef webcamActorRef = system.actorOf(props);
        Publisher<Frame> localActorPublisher = AbstractActorPublisher.create(webcamActorRef);
        return Source.fromPublisher(localActorPublisher);
    };

    public static Future<Source<opencv_core.Mat, Object>> remoteWebCam(RemoteProvider provider, ActorSystem system, Materializer materializer) {
        ByteString beginFrame = ByteString.fromInts(0xff, 0xd8);
        ByteString endFrame = ByteString.fromInts(0xff, 0xd9);

        RemoteProvider remoteProvider = new RemoteProvider();
        HttpRequest httpRequest = HttpRequest.create(remoteProvider.getUrl());
        CompletableFuture<Source<ByteString, Object>> eventualChunks = Http
                .get(system)
                .singleRequest(httpRequest, materializer)
                .toCompletableFuture().thenApply((func) ->
                        func.entity().getDataBytes()
                );

        return eventualChunks.thenApply(a ->
                a.via(new FrameChunker(beginFrame, endFrame).withAttributes(Attributes.logLevels(Logging.InfoLevel(), Logging.InfoLevel(), Logging.InfoLevel())))
                        .via(bytesToFile("content.data"))
                        .via(bytesToMat(provider))
        );
    }

    private static Flow<ByteString, ByteString, NotUsed> bytesToFile(String filename) {
        return Flow.<ByteString>create()
                .map(content -> {
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(filename));
                    bufferedOutputStream.write(content.toArray());
                    return content;
                });
    }


    private static Flow<ByteString, opencv_core.Mat, NotUsed> bytesToMat(RemoteProvider provider) {
        return Flow.<ByteString>create()
                .map(c -> c.toArray())
                .map(bytes -> {
                    opencv_core.CvSize frameSize = new opencv_core.CvSize(provider.width, provider.height);
                    opencv_core.IplImage image = opencv_core.cvCreateImage(frameSize, opencv_core.CV_8UC3, 3);
                    image.imageData(new BytePointer(bytes));
                    return image;
                }).map(MediaConversion::iplImageToFrame)
                .map(MediaConversion::toMat)
                .map(i -> opencv_imgcodecs.imdecode(i, opencv_imgcodecs.CV_LOAD_IMAGE_COLOR));
    }
}
