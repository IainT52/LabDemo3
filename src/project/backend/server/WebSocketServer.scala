package project.backend.server

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.corundumstudio.socketio.listener.{ConnectListener, DataListener, DisconnectListener}
import com.corundumstudio.socketio.{AckRequest, Configuration, SocketIOClient, SocketIOServer}
import play.api.libs.json._
import project.backend.{AddParty, BattleEnded, BattleStarted, BattleState, CharacterTurnReady, Login, LoginResponse, MoveParty, OverworldMap, OverworldState, Register, RegistrationResult, RemoveParty, SaveGame, StopParty, TurnAction, Update, partyJSON}
import project.backend.authenticationsystem.AuthenticationSystem
import project.backend.server.ServerState._
import project.backend.battlesystem._
import project.backend.overworldsystem.OverworldSystem

import scala.collection.mutable.ListBuffer


class WebSocketServer() extends Actor {

  var socketTo_Status: Map[SocketIOClient, String] = Map()
  var socketTo_PartyId: Map[SocketIOClient, String] = Map()
  var partyIdTo_Socket: Map[String, SocketIOClient] = Map()
  var usernameTo_PartyId: Map[String, String] = Map()
  var partyIdTo_Username: Map[String, String] = Map()
  var socketTo_username: Map[SocketIOClient, String] = Map()
  var username_ToSocket: Map[String, SocketIOClient] = Map()
  var overworldMapJSON: String = ""

  val config: Configuration = new Configuration {
    setHostname("localhost")
    setPort(8080)
  }

  val server: SocketIOServer = new SocketIOServer(config)

  server.start()


  var serverState: State = new not_LoggedIn(this)
  val battleSystem: ActorRef = this.context.actorOf(Props(classOf[BattleSystem], self))
  var battleState: Map[String, Boolean] = Map("party_1" -> false)
  val overWorld_System: ActorRef = this.context.actorOf(Props(classOf[OverworldSystem], self))
  var overWorld_List: ListBuffer[String] = ListBuffer()
  val authSystem: ActorRef = this.context.actorOf(Props(classOf[AuthenticationSystem], self))

  server.addConnectListener(
    (socket: SocketIOClient) => {
      socketTo_Status += (socket -> "connected")
//      val partyId = "party_" + (Math.random() * 10000).intValue()
      val partyId = "party_52" // for test purposes only
      socket.sendEvent("yourPartyId", partyId)
      socketTo_PartyId += socket -> partyId
      partyIdTo_Socket += partyId -> socket
    }
  )

  server.addDisconnectListener(
    (socket: SocketIOClient) => {
      battleSystem ! RemoveParty
      overWorld_System ! RemoveParty
    }
  )

  server.addEventListener("register", classOf[String],
    (socket: SocketIOClient, username_Password: String, ackRequest: AckRequest) => {
      val parsed: JsValue = Json.parse(username_Password)
      val username: String = (parsed \ "username").as[String]
      val password: String = (parsed \ "password").as[String]
      socketTo_username += socket -> username
      username_ToSocket += username-> socket
      usernameTo_PartyId += username -> socketTo_PartyId(socket)
      partyIdTo_Username += socketTo_PartyId(socket) -> username
      if(socketTo_Status(socket) != "loggedIn" && socketTo_Status(socket) != "registered"){
        authSystem ! Register(username,password)
      }
      if(socketTo_Status(socket) == "registered"){
        println("Already Registered")
      }
    }
  )

  server.addEventListener("login", classOf[String],
    (socket: SocketIOClient, username_Password: String, ackRequest: AckRequest) => {
      val parsed: JsValue = Json.parse(username_Password)
      val username: String = (parsed \ "username").as[String]
      val password: String = (parsed \ "password").as[String]
      if(socketTo_Status(socket) != "loggedIn" && socketTo_Status(socket) == "registered"){
        authSystem ! Login(username,password)
      }
      if(socketTo_Status(socket) == "loggedIn"){
        println("Already Logged In")
      }
      if(socketTo_Status(socket) == "connected"){
        println("Need to register")
      }
    }
  )

  server.addEventListener("overworldMovement", classOf[String],
    (socket: SocketIOClient, XandY: String, ackRequest: AckRequest) => {
      val parsed: JsValue = Json.parse(XandY)
      val username: String = (parsed \ "username").as[String]
      val x: String = (parsed \ "x").as[String]
      val y: String = (parsed \ "y").as[String]
      if(overWorld_List.contains(socketTo_PartyId(socket))){
        if(x.toDouble == 0 && y.toDouble == 0){
//          println("Your party, " + socketTo_PartyId(socket) + " has stopped moving")
          overWorld_System ! StopParty(socketTo_PartyId(socket))
        }
        else{
//          println("Your party, " + socketTo_PartyId(socket) + " has moved " + x + " in the x direction and " + y + " in the y direction")
          overWorld_System ! MoveParty(socketTo_PartyId(socket),x.toDouble,y.toDouble)
        }
      }
    }
  )
  server.addEventListener("battleAction", classOf[String],
    (socket: SocketIOClient, data: String, ackRequest: AckRequest) => {
      if(battleState.contains(socketTo_PartyId(socket))) {
        if (battleState(socketTo_PartyId(socket))) {
          val parsed: JsValue = Json.parse(data)
          val sourceName: String = (parsed \ "source").as[String]
          val targetName: String = (parsed \ "target").as[String]
          val action: String = (parsed \ "action").as[String]
          battleSystem ! TurnAction(socketTo_PartyId(socket), sourceName, targetName, action)
          println(socketTo_PartyId(socket) + "'s " + sourceName + " has attacked " + targetName + " with " + action)
        }
        else{println("PartyId: " + socketTo_PartyId(socket) + " not in battle")}
      }
      else{println("PartyId: " + socketTo_PartyId(socket) + " not in battle")}
    }
  )
  server.addEventListener("battleStart", classOf[String],
    (socket: SocketIOClient, data: String, ackRequest: AckRequest) => {
      val parsed: JsValue = Json.parse(data)
      val partyId1: String = (parsed \ "partyId1").as[String]
      val partyId2: String = (parsed \ "partyId2").as[String]
      if(!battleState(partyId2)) {
        battleState += partyId1 -> true
        battleState += partyId2 -> true
        println("User: " + socketTo_username(socket) + " with PartyId: " + partyId1 + " has gone into battle with " + partyId2)
      }
      else{
        println("User: " + socketTo_username(socket) + "is in battle")
      }
    }
  )
  server.addEventListener("battleEnd", classOf[String],
    (socket: SocketIOClient, data: String, ackRequest: AckRequest) => {
      val parsed: JsValue = Json.parse(data)
      val partyId1: String = (parsed \ "partyId1").as[String]
      val partyId2: String = (parsed \ "partyId2").as[String]
      if(battleState(partyId1)) {
        battleState += partyId1 -> false
        battleState += partyId2 -> false
        println("User: " + socketTo_username(socket) + " with PartyId: " + partyId1 + " has defeated " + partyId2)
      }
      else{
        println("User: " + socketTo_username(socket) + "is not in battle")
      }
    }
  )
  server.addEventListener("battleState", classOf[String],
    (socket: SocketIOClient, data: String, ackRequest: AckRequest) => {
      val parsed: JsValue = Json.parse(data)
      val partyId1: String = (parsed \ "partyId1").as[String]
      val partyId2: String = (parsed \ "partyId2").as[String]
      val partyJSON1: String = (parsed \ "partyJSON1").as[String]
      val partyJSON2: String = (parsed \ "partyJSON2").as[String]
      if(battleState(partyId1)){
        battleState += partyId1 -> false
        battleState += partyId2 -> false
        println("User: " + socketTo_username(socket) + " with PartyId: " + partyId1 + " is currently in battle with " + partyId2)
        println(partyId1 + " 's Party State is: " + partyJSON1)
        println(partyId2 + " 's Party State is: " + partyJSON2)
      }
      else{
        println("User: " + socketTo_username(socket) + " with PartyId: " + partyId1 + "is not in battle")
        println(partyId1 + " 's Party State is: " + partyJSON1)
        println(partyId2 + " 's Party State is: " + partyJSON2)
      }
    }
  )


  override def receive: Receive = {

    case update: Update =>
      server.getBroadcastOperations.sendEvent("allParties", Json.stringify(Json.toJson(partyIdTo_Socket.keys)))
      battleSystem ! update
      overWorld_System ! update

    case partyJSON(party: String) =>
      val parsed: JsValue = Json.parse(party)
      val userName: String = (parsed \ "username").as[String]
      val characterJSON: String = (parsed \ "characters").as[String]
      authSystem ! SaveGame(userName, characterJSON)
    // Need to remove from data structures

    case RegistrationResult(username: String, success: Boolean, message: String) =>
      username_ToSocket(username).sendEvent(message)
      if (success) {
        println("Username: " + username)
        socketTo_Status += (username_ToSocket(username) -> "registered")
      }
      println(message)

    case LoginResponse(username: String, authenticated: Boolean, message: String) =>
      username_ToSocket(username).sendEvent(message)
      if (authenticated) {
        println(message)
        println("Username: " + username)
        println("PartyId:" + usernameTo_PartyId(username))
        socketTo_Status += (username_ToSocket(username) -> "loggedIn")
        overWorld_List += usernameTo_PartyId(username)
        battleSystem ! AddParty(usernameTo_PartyId(username), "partyJSON")
        overWorld_System ! AddParty(usernameTo_PartyId(username), "partyJSON")
        //////////////MOre shit?/////////////
      }

    case battleStart: BattleStarted =>
      partyIdTo_Socket(battleStart.partyId1).sendEvent("battleStarted")
      partyIdTo_Socket(battleStart.partyId2).sendEvent("battleStarted")
      battleState += battleStart.partyId1 -> true
      battleState += battleStart.partyId2 -> true

    case battleEnd: BattleEnded =>
      partyIdTo_Socket(battleEnd.losingPartyId).sendEvent("battleEnded")
      partyIdTo_Socket(battleEnd.winningPartyId).sendEvent("battleEnded")
      battleState += battleEnd.losingPartyId -> false
      battleState += battleEnd.winningPartyId -> false

    case battleState: BattleState =>
      partyIdTo_Socket(battleState.partyId1).sendEvent("battleState", Json.stringify(Json.toJson(
        Map("playerParty" -> battleState.partyJSON1, "enemyParty" -> battleState.partyJSON2)
      )))
      partyIdTo_Socket(battleState.partyId2).sendEvent("battleState", Json.stringify(Json.toJson(
        Map("playerParty" -> battleState.partyJSON2, "enemyParty" -> battleState.partyJSON1)
      )))
    case overworldMap: OverworldMap => overworldMapJSON = overworldMap.mapJSON

    case overworldState: OverworldState =>
      server.getBroadcastOperations.sendEvent("overworldState", overworldState.overworldStateJSON)

    case turnReady: CharacterTurnReady =>
      partyIdTo_Socket(turnReady.partyId).sendEvent("turnReady", turnReady.characterName)

  }
  override def postStop(): Unit = {
    println("stopping server")
    server.stop()
  }
}

object WebSocketServer {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem()

    import actorSystem.dispatcher

    import scala.concurrent.duration._

    val server = actorSystem.actorOf(Props(classOf[WebSocketServer]))

    actorSystem.scheduler.schedule(0.milliseconds, 33.milliseconds, server, Update(System.nanoTime()))
  }
}




