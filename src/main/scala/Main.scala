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

      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())

      implicit val system = ActorSystem("MazurDBClusterSystem", config)
      val clusterMember = system.actorOf(Props[ClusterMember], name = "clusterMember")
      val storage = system.actorOf(Storage.props, name = "storage")

      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher     

      implicit val timeout = Timeout(5 seconds)
      lazy val route =
        path("cluster") {
          get {
	    implicit val timeout = Timeout(5 seconds)
	    val membersFuture = (clusterMember ? GetMembers).mapTo[Members]
	    complete(membersFuture.map{m => m.members.map{x => x.address.hostPort}.mkString("\n")})
          }
        } ~
        path("storage" / Segment) { bucketName =>
          put { //create
            complete {
              (storage ? Storage.CreateBucket(bucketName)).mapTo[Storage.Result].map{x=>x.success.toString}
	    }
	  } ~
	  delete { //remove
            complete {
              (storage ? Storage.RemoveBucket(bucketName)).mapTo[Storage.Result].map{x=>x.success.toString}
            }
	  }
	} ~
        path("storage" / Segment / IntNumber) { (bucketName, item)=> //contains
          get {
            complete {
              (storage ? Storage.Contains(bucketName, item)).mapTo[Storage.Result].map{x=>x.success.toString}
	    }
          }
        } ~ 
        path("storage" / Segment / IntNumber) { (bucketName, item)=> //insert
          post {
	    complete {
              (storage ? Storage.Insert(bucketName, item)).mapTo[Storage.Result].map{x=>x.success.toString}
	    }
	  }
        }

      val bindingFuture = Http().bindAndHandle(route, "localhost", httpPort.toInt)
      println(s"HTTP server online at http://localhost:$httpPort/\nPress RETURN to stop...")
      scala.io.StdIn.readLine() 
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    }
  }

}