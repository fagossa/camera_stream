package fr.xebia.streams

import akka.util.ByteString
import fr.xebia.streams.DataSamples._
import fr.xebia.streams.transform.Implicits
import org.scalatest._

class ByteStringUtilsSpec extends FlatSpec with MustMatchers {

  import Implicits._

  "A trimmer" should "detect values in different positions" in {
    foundAtBeginning.sliceChunk(inner, outer) mustBe Some(inner ++ outer, ByteString(0x55))
    foundInTheMiddle.sliceChunk(inner, outer) mustBe Some(inner ++ ByteString(0x11) ++ outer, ByteString(0x44))
    foundAtTheEnd.sliceChunk(inner, outer) mustBe Some(inner ++ outer, ByteString.empty)
  }

  it should "detect limits not present" in {
    innerNotPresent.sliceChunk(inner, outer) mustBe None
    outerNotPresent.sliceChunk(inner, outer) mustBe None
  }

}

object DataSamples {
  val inner = ByteString(0xff, 0xd8)

  val outer = ByteString(0xff, 0xd9)

  val foundAtBeginning = inner ++ outer ++ ByteString(0x55)

  val foundInTheMiddle = ByteString(0x22) ++ inner ++ ByteString(0x11) ++ outer ++ ByteString(0x44)

  val foundAtTheEnd = ByteString(0x33) ++ inner ++ outer

  val innerNotPresent = ByteString(0x33) ++ outer ++ ByteString(0x55)

  val outerNotPresent = ByteString(0x33) ++ inner ++ ByteString(0x55)
}
