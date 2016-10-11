name := """MazurDB"""

version := "0.1.0"

scalaVersion := "2.11.7"

import Common._

resolvers ++= Seq (
  Akka.resolver
)

libraryDependencies ++= Seq(
  Akka.actor,
  Akka.remote,
  Akka.protobuf,
  Akka.persistence,
  Akka.cluster,
  Akka.cluster_metrics,
  Akka.cluster_tools,
  Akka.multi_node_testkit,
  Akka.testKit,
  Akka.persistencePluginCasbah,
  Akka.persistencePluginInMemory % "test"
)

scalacOptions ++= Seq("-feature", "-deprecation")

test in assembly := {}

parallelExecution in Test := false