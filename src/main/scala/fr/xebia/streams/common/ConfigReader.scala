package fr.xebia.streams.common

import akka.actor.ActorSystem

trait ConfigReader {

  def host(implicit system: ActorSystem): String =
    system.settings.config.getString("http.host")

}

object ConfigReader extends ConfigReader
