package org.mazurdb

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props

object TheApp {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("14725", "14726", "0"))
    else
      startup(args)
  }

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      // Override the configuration of the port
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())

      // Create an Akka system
      val system = ActorSystem("MazurDBClusterSystem", config)
      // Create an actor that handles cluster domain events
      system.actorOf(Props[ClusterMember], name = "clusterMember")
    }
  }

}