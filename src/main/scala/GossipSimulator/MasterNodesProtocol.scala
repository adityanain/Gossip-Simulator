package GossipSimulator
import akka.actor._

/**
 * Created by Aditya on 10/5/2014.
 */
sealed trait MasterNodesProtocol
case class NodeGotRumor(transmitter : ActorRef) extends MasterNodesProtocol
case object IAmDone extends  MasterNodesProtocol
case object NodeStopping extends MasterNodesProtocol
