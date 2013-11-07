name := """akka-contextual-actors"""

version := "1.0"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.2.3",
    "com.typesafe.akka"   %% "akka-slf4j"       % "2.2.3",
    "ch.qos.logback"      % "logback-classic"  % "1.0.13"
)
