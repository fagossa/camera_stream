package fr.xebia.streams.common

trait ConfigReader {
  import com.typesafe.config._

  private val config: Config = ConfigFactory.defaultApplication()

  def host: String = config.getString("http.host")

}

object ConfigReader extends ConfigReader
