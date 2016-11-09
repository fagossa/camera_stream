package fr.xebia.streams.video

sealed trait RemoteProvider {
  def uri: String
}

case class RPiCamWebInterface(host: String) extends RemoteProvider {
  override def uri: String = s"http://$host/html/cam_pic_new.php"
}
