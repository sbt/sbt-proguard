import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtProguard.autoImport._
import com.typesafe.sbt.SbtProguard

object SampleBuild extends Build {
  import ProguardOptions.keepMain
  import ProguardMerge.append

  lazy val proguardAkka = Project(
    id = "proguard-akka",
    base = file("."),
    configurations = SbtProguard.projectConfigurations,
    settings = Defaults.defaultSettings ++ SbtProguard.projectSettings ++ Seq(
      scalaVersion := "2.10.6",
      libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.1.2",//"2.3.9",
      merge in Proguard := true,
      mergeStrategies in Proguard += append("reference.conf"),
      options in Proguard += keepMain("A"),
      options in Proguard += keepMain("B"),
      options in Proguard += "-dontoptimize", // reduce time for proguard
      options in Proguard += "-dontobfuscate",
      options in Proguard += ProguardConf.akka
    )
  )
}

object ProguardConf {

  val akka =
"""

#
# akka
#

-keepclassmembernames class * implements akka.actor.Actor {
  akka.actor.ActorContext context;
  akka.actor.ActorRef self;
}

-keep class * implements akka.actor.ActorRefProvider {
  public <init>(...);
}

-keep class * implements akka.actor.ExtensionId {
  public <init>(...);
}

-keep class * implements akka.actor.ExtensionIdProvider {
  public <init>(...);
}

-keep class akka.actor.SerializedActorRef {
  *;
}

-keep class * implements akka.actor.SupervisorStrategyConfigurator {
  public <init>(...);
}

-keep class * extends akka.dispatch.ExecutorServiceConfigurator {
  public <init>(...);
}

-keep class * implements akka.dispatch.MailboxType {
  public <init>(...);
}

-keep class * extends akka.dispatch.MessageDispatcherConfigurator {
  public <init>(...);
}

-keep class akka.event.Logging*

-keep class akka.event.Logging$LogExt {
  public <init>(...);
}

-keep class akka.remote.DaemonMsgCreate {
  *;
}

-keep class * extends akka.remote.RemoteTransport {
  public <init>(...);
}

-keep class * implements akka.routing.RouterConfig {
  public <init>(...);
}

-keep class * implements akka.serialization.Serializer {
  public <init>(...);
}

-dontwarn akka.remote.netty.NettySSLSupport**
-dontnote akka.**

#
# scala
#

-keepclassmembers class * { ** MODULE$; }

-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinPool {
  long ctl;
}

-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinPool$WorkQueue {
  int runState;
}

-keepclassmembernames class scala.concurrent.forkjoin.LinkedTransferQueue {
  scala.concurrent.forkjoin.LinkedTransferQueue$Node head;
  scala.concurrent.forkjoin.LinkedTransferQueue$Node tail;
  int sweepVotes;
}

-keepclassmembernames class scala.concurrent.forkjoin.LinkedTransferQueue$Node {
  java.lang.Object item;
  scala.concurrent.forkjoin.LinkedTransferQueue$Node next;
  java.lang.Thread waiter;
}

-dontnote scala.xml.**
-dontnote scala.concurrent.forkjoin.ForkJoinPool
-dontwarn scala.**

#
# protobuf
#

-keep class * extends com.google.protobuf.GeneratedMessage {
  ** newBuilder();
}

#
# netty
#

-keep class * implements org.jboss.netty.channel.ChannelHandler

-dontnote org.jboss.netty.util.internal.**
-dontwarn org.jboss.netty.**

#
# uncommons math
#

-dontwarn org.uncommons.maths.random.AESCounterRNG

"""
}