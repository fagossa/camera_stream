package fr.xebia.streams.transform

import akka.util.ByteString

object ByteStringUtils {

  def trimChunk(chunk: ByteString, limit: List[Byte]): ByteString =
    if (chunk.containsSlice(limit)) {
      val (_, after) = chunk.splitAt(chunk.indexOfSlice(limit))
      after
    } else chunk

}
