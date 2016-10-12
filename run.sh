sbt "runMain org.mazurdb.TheApp 14725 14780" > node1.log &
sbt "runMain org.mazurdb.TheApp 14726 14781" > node2.log & 
sbt "runMain org.mazurdb.TheApp 0 14782" > node3.log &