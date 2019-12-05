package project.backend.server

import com.corundumstudio.socketio.{AckRequest, Configuration, SocketIOClient, SocketIOServer}
import io.socket.client.{IO, Socket}
import io.socket.emitter.Emitter
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.TextField
import scalafx.scene.layout.VBox
import scalafx.scene.control.Button
import javafx.event.ActionEvent
import play.api.libs.json._

class testGUI extends Emitter.Listener{
  override def call(objects: Object*): Unit = {
    // Use Platform.runLater when interacting with the GUI
    // This will run your function on the same thread as the GUI allowing access to all GUI elements/variables
    Platform.runLater(() => {})
  }
}

object testGUI_Object extends JFXApp {
  var socket: Socket = IO.socket("http://localhost:8080/")
    socket.on("test", new testGUI)
  socket.connect()
  val winWidth: Double = 800
  val winHeight: Double = 500
  var usernameInputRegister: TextField = new TextField()
  var passwordInputRegister: TextField = new TextField()
  var usernameLogin: TextField = new TextField()
  var passwordLogin: TextField = new TextField()
  var overworldXMove: TextField = new TextField()
  var overworldYMove: TextField = new TextField()

  val register: Button = new Button {
    text = "Register"
    onAction = (event: ActionEvent) => {
      socket.emit("register", Json.stringify(Json.toJson(Map("username" -> usernameInputRegister.text.value,
        "password" -> passwordInputRegister.text.value))))
    }
  }

  val login: Button = new Button {
    text = "Login"
    onAction = (event: ActionEvent) => {
      socket.emit("login", Json.stringify(Json.toJson(Map("username" -> usernameLogin.text.value, "password" -> passwordLogin.text.value))))
    }
  }

  val overworldMovement: Button = new Button {
    text = "OverWorld Movement"
    onAction = (event: ActionEvent) => {
      socket.emit("overworldMovement", Json.stringify(Json.toJson(Map("username" -> usernameLogin.text.value, "x" -> overworldXMove.text.value, "y" -> overworldYMove.text.value))))
    }
  }
  val battleAction: Button = new Button {
    text = "Battle Action"
    onAction = (event: ActionEvent) => {
      socket.emit("battleAction", Json.stringify(Json.toJson(Map("source" -> "Wizard", "target" -> "Giant", "action" -> "FireBall"))))
    }
  }

  val startBattle: Button = new Button {
    text = "Start Battle"
    onAction = (event: ActionEvent) => {
      socket.emit("battleStart", Json.stringify(Json.toJson(Map("partyId1" -> "party_52", "partyId2" -> "party_1"))))
    }
  }

  val endBattle: Button = new Button {
    text = "End Battle"
    onAction = (event: ActionEvent) => {
      socket.emit("battleEnd", Json.stringify(Json.toJson(Map("partyId1" -> "party_52", "partyId2" -> "party_1"))))
    }
  }

  var status: Button = new Button {
    text = "Party State"
    onAction = (event: ActionEvent) => {
      socket.emit("battleState", Json.stringify(Json.toJson(Map("partyId1" -> "party_52", "partyJSON1" -> "Name: Wizard, Health: 60, Max Health: 100, BattleOptions: lightningBolt, FireBall", "partyId2"->"party_1", "partyJSON2" -> "Name: Giant, Health: 500, Max Health: 500, BattleOptions: groundShake, fistSlam" ))))
    }
  }

  val elements = List(usernameInputRegister, passwordInputRegister, register, usernameLogin, passwordLogin, login, overworldXMove, overworldYMove, overworldMovement, battleAction, startBattle, endBattle, status)
  val contents: VBox = new VBox {
    children = List()
    children = elements
  }

  this.stage = new PrimaryStage{
    title = "testGUI"
    scene = new Scene(winHeight, winHeight){
      content = List(contents)
    }
  }


}
