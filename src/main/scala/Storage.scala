package org.mazurdb

import akka.actor._
import akka.persistence._

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

class Storage(persId:String) extends PersistentActor with ActorLogging {
  import Storage._

  type Set = collection.mutable.Set[Int]
  type BucketType = collection.mutable.Map[String, Set]
  
  var buckets: BucketType = collection.mutable.Map[String, Set]()

  override def persistenceId = persId

  val receiveRecover: Receive = {
    case evt: Command                           => updateBuckets(evt)
    //TODO add snapshot support
    //case SnapshotOffer(_, snapshotBucketType) => buckets = snapshot.asInstanceOf[BucketType]
  }


  //TODO do refactoring
  def updateBuckets(cmd:Command) = cmd match {
    case CreateBucket(name:String) => {
      sender ! (buckets.get(name) match {
        case Some(bucket) => {
	  log.info(s"""Bucket "$name" already exists""")
          Result(false)
	}
	case None => {
          buckets += (name -> collection.mutable.Set.empty[Int])
	  log.info(s"""Create bucket "$name"""")
          Result(true)
        }
      })
    }

    case RemoveBucket(name:String) => {
      sender ! (buckets.get(name) match {
        case Some(bucket) => {
          buckets -= name
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
      sender ! (buckets.get(name) match {
        case Some(bucket) => {
          bucket += data
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

    case Contains(name:String, data:Int) => {
      sender ! (buckets.get(name) match {
        case Some(bucket) => {
          Result(bucket.contains(data))
	}
	case None => {
	  log.info(s"""Bucket "$name" do not exists""")
          Result(false)
        }
      })
    }
  }
/*
  override def receive = {
*/
}