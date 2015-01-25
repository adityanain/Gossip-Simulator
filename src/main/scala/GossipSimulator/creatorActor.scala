package GossipSimulator

import akka.actor.{ Props, ActorRef, ActorSystem, Actor }

import scala.util.Random

/**
 * Created by Aditya on 10/3/2014.
 */
class creatorActor(numNodes: String, topology: String, algorithm: String) extends Actor {
  var numN = numNodes.toInt
  var mapOfActor = scala.collection.mutable.Map.empty[String, ActorRef]
  val networkSystem = ActorSystem("Network")
  var count = 0

  def getRandomActor: ActorRef = {
    val nActors = mapOfActor.size
    var randomInt = Random.nextInt() % nActors
    while (randomInt <= 0)
      randomInt = Random.nextInt % nActors
    val actorRef = mapOfActor("Node" + randomInt.toString)
    //println("random actor is : " + actorRef)
    actorRef
  }

  def randomNeighbor(i: Int, numN: Int): Int = {
    val nActors = numN
    var randomInt = Random.nextInt() % nActors
    while (randomInt <= 0 || randomInt == i) {
      randomInt = Random.nextInt % nActors
    }
    //println("Randdom neigh for" + i + " is " + randomInt )
    randomInt
  }

  topology match {
    case "full" => {
      for (i <- 1 to numN) {
        val nodeRef = networkSystem.actorOf(Props(new Node(algorithm)), "Node" + i.toString)
        mapOfActor += ("Node" + i.toString -> nodeRef)
        val msg = "full->I=Node" + i.toString + "->N=" + numN.toString
        nodeRef ! msg
      }
    }

    case "2D" => {
      val ceilSR = math.ceil(math.sqrt(numN)).toInt
      //println(ceilSR)
      numN = ceilSR * ceilSR
      //println(numN)
      for (i <- 1 to numN) {
        val nodeRef = networkSystem.actorOf(Props(new Node(algorithm)), "Node" + i.toString)
        mapOfActor += ("Node" + i.toString -> nodeRef)
        var msg = "2D->I=Node" + i.toString
        if ((i % ceilSR) == 1)
          msg = msg + "->" + "N=Node" + (i + 1).toString
        else if (i % ceilSR == 0)
          msg = msg + "->" + "N=Node" + (i - 1).toString
        else
          msg = msg + "->" + "N=Node" + (i - 1).toString + "->N=Node" + (i + 1).toString
        if (i + ceilSR < numN)
          msg = msg + "->" + "N=Node" + (i + ceilSR).toString
        if (i - ceilSR > 0)
          msg = msg + "->" + "N=Node" + (i - ceilSR).toString

        //println(msg)
        nodeRef ! msg
      }
    }

    case "line" => {
      for (i <- 1 to numN) {
        val nodeRef = networkSystem.actorOf(Props(new Node(algorithm)), "Node" + i.toString)
        mapOfActor += ("Node" + i.toString -> nodeRef)
        var msg = "line->I=Node" + i.toString
        if (i - 1 > 0) msg = msg + "->N=Node" + (i - 1).toString
        if (i + 1 <= numN) msg = msg + "->N=Node" + (i + 1).toString
        //println(msg)
        nodeRef ! msg
      }
    }
    case "imp2D" => {
      val ceilSR = math.ceil(math.sqrt(numN)).toInt
      //println(ceilSR)
      numN = ceilSR * ceilSR
      //println(numN)
      for (i <- 1 to numN) {
        val nodeRef = networkSystem.actorOf(Props(new Node(algorithm)), "Node" + i.toString)
        mapOfActor += ("Node" + i.toString -> nodeRef)
        var msg = "imp2D->I=Node" + i.toString
        if ((i % ceilSR) == 1)
          msg = msg + "->" + "N=Node" + (i + 1).toString
        else if (i % ceilSR == 0)
          msg = msg + "->" + "N=Node" + (i - 1).toString
        else
          msg = msg + "->" + "N=Node" + (i - 1).toString + "->N=Node" + (i + 1).toString
        if (i + ceilSR < numN)
          msg = msg + "->" + "N=Node" + (i + ceilSR).toString
        if (i - ceilSR > 0)
          msg = msg + "->" + "N=Node" + (i - ceilSR).toString

        val randomN = randomNeighbor(i, numN)
        msg = msg + "->" + "N=Node" + randomN.toString

        nodeRef ! msg
      }
    }
  }

  var count10Array = new Array[String](numN)
  val b = System.currentTimeMillis()

  algorithm match {
    case "gossip" => {
      val rumor = "Ebola is dangerous. Be aware!"
      val randNode = getRandomActor

      //mapOfActor.foreach(_)
      randNode ! "rumor->" + rumor
    }
    case "push-sum" => {
      val valString = "push-sum->"
    }
  }

  def receive = {
    case NodeGotRumor(transmitter: ActorRef) => {
      //println(sender.path.name + " got the rumor from " + transmitter.path.name)
      count += 1
      // Every Node in the Network got the message. Convergence Achieved!
      if (count == numN) {
        println("The time taken for convergence using " + algorithm + " algorithm is : " + (System.currentTimeMillis() - b) + " ms")
        context.children.foreach(context.stop(_))
        context stop self
        System.exit(0)
      }
    }
    case IAmDone => {
      //println(sender.path.name + " received the message 10 times")
      count10Array +: (sender.path.name)
    }

    case msgString: String => {
      val msgArr = msgString.split("->")
      msgArr(0) match {
        case "ConvergedValue" => {
          val s = msgArr(1).split("=")(1).toDouble
          val w = msgArr(2).split("=")(1).toDouble
          val ratio = s / w
          println(sender.path.name + " converged at ratio = " + ratio.toString)
          //println(sender.path.name + "converged at : s = " + msgArr(1).split("=")(1) + " , w = " + msgArr(2).split("=")(1))

          count += 1
          if (count > 1) {
            println("The time taken for convergence using " + algorithm + " algorithm is : " + (System.currentTimeMillis() - b) + "ms")
            context.children.foreach(context.stop(_))
            context stop self
            System.exit(0)
          }
          /*          if (count >= 0.95 * numN) {
            println("More than 95% of the nodes have converged")
          }*/
          if (count == numN - 1) {
            println("Entire Network Converged")
          }
        }

      }
    }

    case _ => println("What crap I got from : " + sender + "?")
  }
}