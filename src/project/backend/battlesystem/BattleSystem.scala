package project.backend.battlesystem

import akka.actor.{Actor, ActorRef}
import project.backend.BattleState


class BattleSystem(server: ActorRef) extends Actor {


  override def receive: Receive = {
    case battleState: BattleState =>


  }

}
