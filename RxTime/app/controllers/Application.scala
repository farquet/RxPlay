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
    
    val timeObs = Observable.interval(1 second).map(_ =>
     new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance.getTime)  
    )
    
   
    
    def processClientData(func: String, arg:String, m:WidgetManager) = {
      func match {
        case "start" => 
         m.subscribePush("time")
        case "stop" =>
          m.unsubscribe("time")
        case _ =>
          println("unrecognized token")
      }
    }
    
    def onClientClose(m:WidgetManager) = {
      m.unsubscribe("time")
    }
    
    val manager = new WidgetManager(processClientData, onClientClose)
    
    manager.addObservable("time", timeObs)
    
    manager.webSocket
  }
  
  def index = Action {
    Ok(views.html.index())
  }

}
