package project.backend.overworldsystem

import akka.actor.{Actor, ActorRef}
import project.backend.{MoveParty, StopParty}


class OverworldSystem(server: ActorRef) extends Actor {

  override def receive: Receive = {
    case stopParty: StopParty =>
      println("Your party, " + stopParty.partyId + " has stopped moving")
    case moveParty: MoveParty =>
      println("Your party, " + moveParty.partyId + " has moved " + moveParty.x.toString + " in the x direction and " + moveParty.y.toString + " in the y direction")
  }

}
