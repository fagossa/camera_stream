package fr.xebia.streams.java.video;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.actor.AbstractActorPublisher;
import akka.stream.javadsl.Source;
import fr.xebia.streams.java.common.Dimensions;
import fr.xebia.streams.video.LocalCamFramePublisher;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.reactivestreams.Publisher;

public class Local {


    public static WebCam localWebCam = (Integer deviceId,
                  Dimensions dimensions,
                  Integer bitsPerPixel,
                  FrameGrabber.ImageMode imageMode, ActorSystem system) -> {
        Props props = LocalCamFramePublisher.props(deviceId, dimensions.getWidth(), dimensions.getHeight(), bitsPerPixel, imageMode);
        ActorRef webcamActorRef = system.actorOf(props);
        Publisher<Frame> localActorPublisher = AbstractActorPublisher.create(webcamActorRef);
        Source<Frame, NotUsed> frameNotUsedSource = Source.fromPublisher(localActorPublisher);
        return frameNotUsedSource;
    };
}
