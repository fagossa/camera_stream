package fr.xebia.streams.transform

import akka.util.ByteString
import org.slf4j.LoggerFactory

object ByteStringUtils {

  val logger = LoggerFactory.getLogger(getClass)

  def trimChunk(chunk: ByteString, limit: List[Byte]): ByteString = {
    if (chunk.containsSlice(limit)) {
      if (chunk.toList.startsWith(limit)
        || chunk.toList.endsWith(limit)) {
        ByteString(removeSubList[Byte](chunk.toList, limit): _*)
      } else {
        val reverse = chunk.reverse
        val (before, _) = reverse.splitAt(reverse.indexOfSlice(limit.reverse))
        before.reverse
      }
    } else chunk
  }

  def removeSubList[T](list: List[T], subList: List[T]): List[T] =
    if (list.containsSlice(subList))
      removeSubList(list.diff(subList), subList)
    else list

}
