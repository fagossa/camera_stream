package fr.xebia.streams.java.video;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Source;
import fr.xebia.streams.java.common.Dimensions;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

@FunctionalInterface
public interface WebCam {

    public Source<Frame, NotUsed> apply(
            Integer deviceId,
            Dimensions dimensions,
            Integer bitsPerPixel,
            FrameGrabber.ImageMode imageMode,ActorSystem system
    );
}
