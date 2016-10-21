name := """MazurDB"""

version := "0.1.0"

scalaVersion := "2.11.7"

import Common._

resolvers ++= Seq (
  Akka.resolver,
  Spray.resolver
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
  Akka.http,
  Akka.http_json,
  Akka.ddata,
  //Akka.persistencePluginCasbah,
  //Akka.persistencePluginInMemory % "test"
  Akka.persistencePluginLevelDB,
  Akka.persistencePluginLevelDBJNI
)

libraryDependencies ++= Seq(
  Spray.http,
  Spray.httpx,
  Spray.json
)

scalacOptions ++= Seq("-feature", "-deprecation")

test in assembly := {}

parallelExecution in Test := false
