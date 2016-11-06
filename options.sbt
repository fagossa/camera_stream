scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xfatal-warnings",
  "-feature",
  "-language:_"
)

val buildSettings = Defaults.coreDefaultSettings ++ Seq(
  javaOptions += "ulimit -c unlimited"
)