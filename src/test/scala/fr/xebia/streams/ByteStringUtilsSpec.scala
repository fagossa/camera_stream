package fr.xebia.streams
import akka.util.ByteString
import fr.xebia.streams.transform.ByteStringUtils
import org.scalatest._

class ByteStringUtilsSpec extends FlatSpec with MustMatchers {

  val separator = ByteString(0xff, 0xd8)

  val foundAtBegining = separator ++ ByteString(0x55)

  val foundInTheMiddle = ByteString(0x22) ++ separator ++ ByteString(0x44)

  val foundAtTheEnd = ByteString(0x33) ++ separator

  "A trimmer" should "detect values in different positions" in {
    ByteStringUtils.trimChunk(foundAtBegining, separator.toList) mustBe ByteString(0x55)
    ByteStringUtils.trimChunk(foundInTheMiddle, separator.toList) mustBe ByteString(0x44)
    ByteStringUtils.trimChunk(foundAtTheEnd, separator.toList) mustBe ByteString(0x33)
  }

}
