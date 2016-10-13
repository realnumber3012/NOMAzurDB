package org.mazurdb

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.Member
import akka.actor.ActorLogging
import akka.actor.Actor

case object GetMembers
case class Members(members: List[Member])

class ClusterMember extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  var members = collection.mutable.Set.empty[Member]

  // subscribe to cluster changes, re-subscribe when restart 
  override def preStart(): Unit = {
    //#subscribe
    members = collection.mutable.Set.empty[Member]
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case state: CurrentClusterState => {
      log.info("Current members: {}", state.members.mkString(", "))
      members ++= state.members
    }
    case MemberUp(member) => {
      log.info("Member is Up: {}", member.address)
      members += member
    }
    case UnreachableMember(member) => {
      log.info("Member detected as unreachable: {}", member)
    }
    case MemberRemoved(member, previousStatus) => {
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
      members -= member
    }
    case GetMembers => {
      sender ! Members(members.toList)
    }
    case _: MemberEvent => // ignore
  }
}