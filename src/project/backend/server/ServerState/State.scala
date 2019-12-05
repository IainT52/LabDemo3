package project.backend.server.ServerState

import project.backend.server.WebSocketServer

abstract class State(server: WebSocketServer) {
  def register(server: WebSocketServer): Unit = {
  }
  def logIn(server: WebSocketServer): Unit = {
  }
}
