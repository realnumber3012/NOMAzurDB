package org.mazurdb

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._


object TheApp {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("14725", "14726", "0"), Seq("14780", "14781", "14782"))
    else
      startup(args.take(args.length/2), args.drop(args.length/2))
  }

  def startup(ports: Seq[String], httpPorts: Seq[String]): Unit = {
    require(ports.length == httpPorts.length)
    (ports zip httpPorts) foreach { case (port, httpPort) =>
      // Override the configuration of the port
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())

      // Create an Akka system
      implicit val system = ActorSystem("MazurDBClusterSystem", config)
      // Create an actor that handles cluster domain events
      val clusterMember = system.actorOf(Props[ClusterMember], name = "clusterMember")

      implicit val materializer = ActorMaterializer()
      // needed for the future map/flatmap in the end
      implicit val executionContext = system.dispatcher     

      val route =
        path("cluster") {
          get {
            //complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
	    implicit val timeout = Timeout(5 seconds)
	    val membersFuture = (clusterMember ? GetMembers).mapTo[Members]
	    complete(membersFuture.map{m => m.members.map{x => x.address.hostPort}.mkString("\n")})
          }
        }

      val bindingFuture = Http().bindAndHandle(route, "localhost", httpPort.toInt)
      println(s"HTTP server online at http://localhost:$httpPort/\nPress RETURN to stop...")
      scala.io.StdIn.readLine() // let it run until user presses return
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
    }
  }

}