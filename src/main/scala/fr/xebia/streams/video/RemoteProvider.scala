package fr.xebia.streams.video

sealed trait RemoteProvider {

  def width: Int

  def height: Int

  def uri: String
}

case class RPiCamWebInterface(host: String) extends RemoteProvider {

  val width = 512

  val height = 288

  override def uri: String = s"http://$host/html/cam_pic_new.php"

}
