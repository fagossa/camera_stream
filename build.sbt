name := "camera_stream"

version := "1.0"

val akkaVersion = "2.4.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-contrib" % "0.4",
  "org.typelevel" %% "cats" % "0.8.0",
  // mapping
  "com.typesafe.play" %% "play-json" % "2.5.8",
  // logs
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback"    %  "logback-classic" % "1.1.3",
  // test
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

// Assembly settings
//mainClass in Global := Some("fr.xebia.streams.GroupedSource")

fork in run := true
