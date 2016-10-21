package org.mazurdb

import akka.actor._
import akka.persistence._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData

import akka.cluster.ddata.GSet
import akka.cluster.ddata.GSetKey
import akka.cluster.ddata.ORSet
import akka.cluster.ddata.ORSetKey
import akka.cluster.ddata.Replicator
import akka.cluster.ddata.Replicator._

object Storage {
  trait Request
  trait Query
  trait Command
  case class CreateBucket(name:String) extends Request with Command
  case class RemoveBucket(name:String) extends Request with Command
  case class Insert(name:String, data:Int) extends Request with Command
  case class Contains(name:String, data:Int) extends Request with Query

  trait Response
  case class Result(success: Boolean) extends Response

  def props(id:String) = Props(new Storage(id))
}

// TODO: write concern
// TODO: element type
// TODO: timeouts
// TODO: read concern
class Storage(persId:String) extends PersistentActor with ActorLogging {
  import Storage._

  val bucketsKey = ORSetKey[String]("buckets")
  //TODO: for simplicity currently, change to pure CRDT
  var localBuckets = collection.mutable.Map[String, GSetKey[Int]]()


  override def persistenceId = persId

  val replicator = DistributedData(context.system).replicator
  implicit val node = Cluster(context.system)

  replicator ! Subscribe(bucketsKey, self)

  val receiveRecover: Receive = {
    case evt: Command                           => updateBuckets(evt)
    //TODO add snapshot support
    //case SnapshotOffer(_, snapshotBucketType) => buckets = snapshot.asInstanceOf[BucketType]
  }

  //TODO do refactoring
  def updateBuckets(cmd:Command) = cmd match {
    case CreateBucket(name:String) => {
      sender ! (localBuckets.get(name) match {
        case Some(bucketKey) => {
          log.info(s"""Bucket "$name" already exists""")
          Result(false)
	      }
	      case None => {
          val newBucketKey = GSetKey[Int](name)
          localBuckets += name -> newBucketKey
          replicator ! Replicator.Update(bucketsKey, ORSet.empty[String], WriteLocal)(_ + name)
          replicator ! Subscribe(newBucketKey, self)
          log.info(s"""Create bucket "$name"""")
          Result(true)
        }
      })
    }

    case RemoveBucket(name:String) => {
      sender ! (localBuckets.get(name) match {
        case Some(bucketKey) => {
          localBuckets -= name
          replicator ! Replicator.Update(bucketsKey, ORSet.empty[String], WriteLocal)(_ - name)
          replicator ! Unsubscribe(bucketKey, self)
	        log.info(s"""Bucket "$name" removed""")
          Result(true)
	      }
	      case None => {
	        log.info(s"""Bucket "$name" do not exists""")
          Result(false)
        }
      })
    }

    case Insert(name:String, data:Int) => {
      sender ! (localBuckets.get(name) match {
        case Some(bucketKey) => {
          replicator ! Replicator.Update(bucketKey, GSet.empty[Int], WriteLocal)(_ + data)
	        log.info(s"""$data inserted into Bucket "$name"""")
          Result(true)
	      }
	      case None => {
	        log.info(s"""Bucket "$name" do not exists""")
          Result(false)
        }
      })
    }
  }

  val receiveCommand: Receive = {
    case msg@CreateBucket(name:String) =>
      persist(msg) (updateBuckets)

    case msg@RemoveBucket(name:String) =>
      persist(msg) (updateBuckets)

    case msg@Insert(name:String, data:Int) =>
      persist(msg) (updateBuckets)

    case _: UpdateResponse[_] => //ignored yet

    case Contains(name:String, data:Int) => {
      sender ! (localBuckets.get(name) match {
        case Some(bucketKey) => {
          implicit val timeout = Timeout(5.seconds)
          import scala.concurrent.ExecutionContext.Implicits.global
          val ans = (replicator ? Get(bucketKey, ReadLocal)).map {
            case g @ GetSuccess(bucket, _) =>
              Result(g.get(bucket).asInstanceOf[GSet[Int]].contains(data))
            case GetFailure(bucket, _) =>
              Result(false)
            case NotFound(bucket, _) =>
              Result(false)
          }
          Await.result(ans, timeout.duration).asInstanceOf[Result]
          //Result(bucket.contains(data))
	      }
	      case None => {
	        log.info(s"""Bucket "$name" do not exists""")
          Result(false)
        }
      })
    } //Contains

    case c @ Changed(dataKey) => {
      c.get(dataKey) match {
        case d: ORSet[_] => { //TODO: scalac bug was here
          val data = d.asInstanceOf[ORSet[String]]
          log.info("Current buckets is {}", data.elements)

          //new buckets
          for(name <- data.elements) {
            if(!localBuckets.contains(name)) {
              localBuckets += (name -> GSetKey[Int](name))
              replicator ! Subscribe(GSetKey[Int](name), self)
            }
          }

          //removed buckets
          for(name <- localBuckets.keys) {
            if(!data.elements.contains(name)) {
              localBuckets -= name
              replicator ! Unsubscribe(GSetKey[Int](name), self)
            }
          }
        }
        case d:GSet[_] => {
          val data = d.asInstanceOf[GSet[Int]]
          log.info(s"Current elements in $dataKey {} are: ", data.elements)
        }
      }
    }
  }
}
