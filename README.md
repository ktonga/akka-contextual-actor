akka-contextual-actor
=====================

A really small library (just a few classes) which lets you trace your actors messages transparently propagating
a common context together with your messages and adding the specified values to the MDC of the underling
logging framework.

## Usage

Download the latest release and copy it into your project's lib folder (I promise to publish it in a repository soon)
Also you will need to add the Typesafe Snapshots Repository since it depends on new stuff available in Akka 2.3-SNAPSHOT

```scala
resolvers += "Typesafe Snapshot Repository" at "http://repo.typesafe.com/typesafe/snapshots/"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3-SNAPSHOT",
    "com.typesafe.akka"   %% "akka-slf4j"       % "2.3-SNAPSHOT",
    "ch.qos.logback"      % "logback-classic"  % "1.0.13"
)
```

### Getting started

### Sample Application

    [Try it out with the sample application](sample/README.md)

