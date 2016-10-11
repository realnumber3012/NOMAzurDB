import sbt._

object Common {

    object Other {
      val typesafeConfig = "com.typesafe" % "config" % "1.3.1"
    }

    object Akka {
      val resolver = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
      val groupId = "com.typesafe.akka"
      val version = "2.4.11"
      val actor = groupId %% "akka-actor" % version
      val remote = groupId %% "akka-remote" % version
      val protobuf = groupId %% "akka-protobuf" % version
      val persistence = groupId %% "akka-persistence" % version
      val cluster = "com.typesafe.akka" %% "akka-cluster" % version
      val cluster_metrics = groupId %% "akka-cluster-metrics" % version
      val cluster_tools = groupId  %% "akka-cluster-tools" % version
      val multi_node_testkit = groupId %%  "akka-multi-node-testkit" % version
      val testKit = groupId %% "akka-testkit" % version % "test"

      val imMemoryPersistenceResolver = "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"
      val persistencePluginCasbah = "com.github.scullxbones" %% "akka-persistence-mongo-casbah" % "1.3.2"
      val persistencePluginInMemory = "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.3.10"
    }


    object Plugins {
      val revolver = "io.spray" % "sbt-revolver" % "0.8.0"
      val assembly = "com.eed3si9n" % "sbt-assembly" % "0.13.0"
    }
}
