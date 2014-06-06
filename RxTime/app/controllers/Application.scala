package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import java.util.Calendar
import java.text._
import scala.concurrent.duration._

import rx.lang.scala._

import models.RxPlay._
import models.WidgetManager

import play.api.libs.iteratee._

object Application extends Controller {

  /**
   * Creates a WebSocket using WidgetManager
   */
  def socket = WebSocket.using[String] { request =>
   
    val timeObs = Observable.interval(1 second).map(_ => new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance.getTime))
    
    def processClientData(func: String, arg:String, m:WidgetManager) = {
      func match { // matching command received from client
        
        case "start" =>
          m.getObservable("time") match {
            case Some(_) =>
              println("starting clock")
              // subscribing to the observable by just pushing data in the webSocket
              // (this will automatically call timeUpdate JS callback on the client side)
              m.subscribePush("time")
            case None =>
              println("Warning : \"time\" Observable doesn't exist.")
          }
          
        case "stop" =>
          println("stopping clock")
          m.unsubscribe("time")
        
        case _ => 
          println("Unrecognized input <"+func+":"+arg+">")
      }
    }
  
    // function that will clean the app when client disconnect
    def onClientClose(m: WidgetManager) = {
      m.unsubscribe("time")
	  m.close // closing manager
    }
  
    val manager = new WidgetManager(processClientData, onClientClose)
 
    // adding the time observable to the widgetManager
    manager.addObservable("time", timeObs)
    
    // return the websocket
    manager.webSocket
  }
  
  def index = Action {
    Ok(views.html.index())
  }

}
