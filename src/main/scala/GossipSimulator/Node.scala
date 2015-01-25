package GossipSimulator

import java.util.concurrent.TimeUnit

import akka.actor._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.util.Random
import akka.dispatch.ExecutionContexts
import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.pow

class Node(algorithm: String) extends Actor {
  //if(algorithm == "push-sum")
  //println("jsdf: " + algorithm )
  var firstMessage = true
  var tickClock: Cancellable = _
  var neighbours = ArrayBuffer[String]()
  var iAm: String = _
  var creatorActor: ActorRef = null
  var rumor: String = _
  var rumorCount: Int = 0
  var s = 0.0
  var w = 1.0
  var swRatio = 0.0
  var prevSWRatio = 0.0
  var ratioDiffCount = 0
  val threshold = 0.0000000001
  //println(threshold)
  var notConverged = true

  override def postStop = {
    if (!tickClock.isCancelled)
      tickClock.cancel()
  }

  def spreadRumor = {
    val msg = "rumor->" + rumor
    val neighboursLen = neighbours.length
    var randNeighIndex = Random.nextInt() % neighboursLen
    while (randNeighIndex < 0)
      randNeighIndex = Random.nextInt() % neighboursLen
    val randNeighbour = context.actorSelection("/user/" + neighbours(randNeighIndex))

    randNeighbour ! msg
  }

  def TransmitSW = {
    val s_ = s / 2
    val w_ = w / 2
    s = s_
    w = w_
    val msg = "push-sum->s=" + s_.toString + "->w=" + w_.toString
    val neighboursLen = neighbours.length
    var randNeighIndex = Random.nextInt() % neighboursLen
    while (randNeighIndex < 0)
      randNeighIndex = Random.nextInt() % neighboursLen
    val randNeighbour = context.actorSelection("/user/" + neighbours(randNeighIndex))
    //println(msg)
    randNeighbour ! msg

  }

  def startPushSum = {
    s = iAm.drop(4).toDouble
    //s = (iAm takeRight 1).toDouble
    //println("s is : " + s)
    tickClock = context.system.scheduler.schedule(30 second, 50 milliseconds, self, ReceiveTimeout)
    //TransmitSW
  }

  def receive = {

    case messageString: String => {
      val msgArray = messageString.split("->")
      msgArray(0) match {
        case "full" => {
          creatorActor = sender()
          iAm = msgArray(1).split("=")(1)
          val myNo = (iAm takeRight 1).toInt
          val numNodes = msgArray(2).split("=")(1)
          for (i <- 1 to numNodes.toInt if i != myNo) {
            neighbours += "Node" + i.toString
          }
          
          if(algorithm equals "push-sum") startPushSum
        }
        case "2D" => {
          creatorActor = sender
          iAm = msgArray(1).split("=")(1)
          val len = msgArray.length
          for (i <- 2 until len) {
            neighbours += msgArray(i).split("=")(1)
          }
          if(algorithm equals "push-sum") startPushSum
        }
        case "line" => {
          //println(messageString)
          creatorActor = sender
          iAm = msgArray(1).split("=")(1)
          val len = msgArray.length
          for (i <- 2 until len) {
            neighbours += msgArray(i).split("=")(1)
          }
          if(algorithm equals "push-sum") startPushSum
        }
        case "imp2D" => {
          creatorActor = sender
          iAm = msgArray(1).split("=")(1)
          val len = msgArray.length
          for (i <- 2 until len) {
            neighbours += msgArray(i).split("=")(1)
          }
          if(algorithm equals "push-sum") startPushSum
        }

        case "rumor" => {
          if (rumorCount == 0) {
            creatorActor ! NodeGotRumor(sender)
          }

          rumorCount += 1

          if (rumorCount < 10) {
            rumor = msgArray(1)

          } else if (rumorCount == 10) {
            tickClock.cancel()
            creatorActor ! IAmDone

          }
          // Start the clock
          if (firstMessage) {
            firstMessage = false
            tickClock = context.system.scheduler.schedule(0 millisecond, 50 milliseconds, self, ReceiveTimeout)
          }
        }
        case "push-sum" => {
          // Store S and W and in the next clock send half of these values
          prevSWRatio = s / w
          s = s + msgArray(1).split("=")(1).toDouble
          w = w + msgArray(2).split("=")(1).toDouble
          swRatio = s / w

          if ((swRatio - prevSWRatio).abs < threshold) {
            ratioDiffCount += 1
          } else
            ratioDiffCount = 0

          if (ratioDiffCount == 3 && notConverged) {
            notConverged = false
            val msg = "ConvergedValue->s=" + s.toString + "->w=" + w.toString
            creatorActor ! msg
            tickClock.cancel()
            //println(iAm + " congerved")
          }
          
          //println(iAm + " got a push-sum message")

        }
      }

    }
    case ReceiveTimeout => {
      if (algorithm != "push-sum" && rumorCount < 10) {
        spreadRumor
      } 
      else if (algorithm == "push-sum") {
        TransmitSW
      }
    }
  }
}
