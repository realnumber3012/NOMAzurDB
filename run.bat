start sbt "-DnodeName=node1" "runMain org.mazurdb.TheApp 14725 14780" > node1.log
start sbt "-DnodeName=node2" "runMain org.mazurdb.TheApp 14726 14781" > node2.log
start sbt "-DnodeName=node3" "runMain org.mazurdb.TheApp 0 14782" > node3.log
