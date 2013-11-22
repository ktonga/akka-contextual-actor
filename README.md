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

#### ActorRef operations

In order to enable tracing within an Actor you just need to mixin with TracingActor
You will now have the contextualized operations analogous to the regular ActorRef operations

* `tellWithCxt` or `!+`
* `forwardWithCxt` or `>+`

Any message you tell or forward from a TracingActor to another TracingActor with be wrapped with the current
message context implicitly picked up and unwrapped for processing it in the target actor.

```scala
class MyTracedActor extends Actor with TracingActor {
  ...
    anotherTracingActorRef !+ SomeMessage
    anotherTracingActorRef >+ ForwardedMessage
  ...
}
```

It is also possible to enable tracing outside actors (a Web Controller or Main object for instance), you need to import
`com.github.ktonga.akka.contextual.actor.Implicits._` and the ActorRefs will get contextualized operations. Then you
have to create an implicit context which will be picked up.

```scala
class MyController {
  import Implicits._
  ...
  def processRequest(params: Params) = {
    implicit val ctx: Option[MsgCtx] = Some(MsgCtx(Map("requestId" -> params.reqId)))
    businessTracingActorRef !+ StartRequest
  }
}
```

#### Akka patterns

In a similar way to regular operations, you have the contextualized version for the common Akka patterns.
You can import `ask` or `pipe` from package `com.github.ktonga.akka.contextual.pattern`, and you will have the
following pattern operators:

* `askWithCxt` or `?+`
* `pipeWithCxt` or `|+`

You will be able to propagate the message context to collaborating actors using the already known patterns.

#### Logging with MDC

Finally you will need to configure SLF4J logging

Add the dependency

    "com.typesafe.akka"   %% "akka-slf4j"       % "2.3-SNAPSHOT",

Add the logger in your application.config

    akka {
        loggers = ["akka.event.slf4j.Slf4jLogger"]
    }

Now you can include message context attributes in your appender pattern

```xml
<pattern>%X{akkaTimestamp} [%X{sourceThread}] [%X{requestId}] %-5level %X{akkaSource} - %msg%n</pattern>
```

See Akka logging documentations for further information http://doc.akka.io/docs/akka/snapshot/scala/logging.html#SLF4J

### Sample Application

[Try it out with the sample application](sample/README.md)

