package project.backend.authenticationsystem

import akka.actor.{Actor, ActorRef}
import project.backend.{ Login, LoginResponse, Register, RegistrationResult}


class AuthenticationSystem(server: ActorRef)  extends Actor{
  var userDatabase: Map[String, String] = Map()
  override def receive: Receive = {
    case register: Register =>
      userDatabase += register.username -> register.password
      sender() ! RegistrationResult(register.username, true, "Registered")
    case login: Login =>
      if(userDatabase.contains(login.username)){
        sender() ! LoginResponse(login.username, true, "Logged In")
        println("Welcome to the Server, your starter class is: Wizard")
        println("Your Party JSON: \"Name: Wizard, Health: 100, Max Health: 100, BattleOptions: lightningBolt, FireBall\"")
        println("MapJSON: \"MapJson\"")
      }
  }


}
