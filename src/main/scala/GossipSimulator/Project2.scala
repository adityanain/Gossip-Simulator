package GossipSimulator

import akka.actor._

/**
 * Created by Aditya on 10/3/2014.
 */
object Project2 {
  def main(args: Array[String]) {
      if(args.length < 3) println("Please provide the following arguments : numNodes topology algorithm")
      else if(args.length >= 4) println("Only 3 arguments please")

    val creatorSystem = ActorSystem("creatorSystem")
    val creator = creatorSystem.actorOf(Props(new creatorActor(args(0), args(1), args(2))), "creator")

  }

}
