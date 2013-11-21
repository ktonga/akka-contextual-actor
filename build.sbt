name := """akka-contextual-actor"""

organization := "com.github.ktonga"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.2"

resolvers += "Typesafe Snapshot Repository" at "http://repo.typesafe.com/typesafe/snapshots/"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3-SNAPSHOT",
    "com.typesafe.akka"   %% "akka-slf4j"       % "2.3-SNAPSHOT",
    "ch.qos.logback"      % "logback-classic"  % "1.0.13"
)
