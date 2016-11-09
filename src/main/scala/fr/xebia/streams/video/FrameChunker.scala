package fr.xebia.streams.video

import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.stream.stage._
import akka.util.ByteString
import fr.xebia.streams.transform.Implicits

class FrameChunker(val beginOfFrame: ByteString, val endOfFrame: ByteString) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in = Inlet[ByteString]("Chunker.in")
  val out = Outlet[ByteString]("Chunker.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var buffer = ByteString.empty

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (isClosed(in)) emitChunk()
        else pull(in)
      }
    })
    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem = grab(in)
        buffer ++= elem
        emitChunk()
      }

      override def onUpstreamFinish(): Unit = {
        if (buffer.isEmpty) completeStage()
        // elements left in buffer, keep accepting downstream pulls
        // and push from buffer until buffer is emitted
      }
    })

    private def emitChunk(): Unit = {
      if (buffer.isEmpty) {
        if (isClosed(in)) completeStage()
        else pull(in)
      } else {
        import Implicits._
        buffer.sliceChunk(beginOfFrame, endOfFrame) match {
          case Some((chunk, remaining)) =>
            buffer = remaining
            push(out, chunk)

          case None =>
            pull(in)
        }
      }
    }

  }
}