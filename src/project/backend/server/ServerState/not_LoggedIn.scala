package project.backend.server.ServerState
import project.backend.server.WebSocketServer

class not_LoggedIn(server: WebSocketServer) extends State (server){
  override def logIn(server: WebSocketServer): Unit = {
  }

  override def register(server: WebSocketServer): Unit = {
  }

}
