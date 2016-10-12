package org.mazurdb

import akka.actor._

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

  def props = Props[Storage]
}

class Storage extends Actor with ActorLogging {
  import Storage._

  type Set = collection.mutable.Set[Int]
  
  val buckets = collection.mutable.Map.empty[String, Set]

  override def receive = {
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
}