package controllers

import play.api._
import play.api.mvc._

//import play.api.libs.Comet
import play.api.libs.iteratee._
//import play.api.libs.concurrent._
import play.api.templates._

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import java.util.Date
import java.text._

import scala.concurrent.Future
import rx.lang.scala._

import RxPlay._

object Application extends Controller {
  
  /** 
   * Implementation of the comet-clock sample using Observables.
   */
  
  val obs: Observable[String] = Observable.interval(100 milliseconds).map(_ => new SimpleDateFormat("HH mm ss").format(new Date))
  
  def liveClock = Action {
    Ok.chunked(CometObs(obs, callback = "parent.clockChanged"))
  }
  
  def index = Action {
    Ok(views.html.index())
  }
}

object CometObs {

  private def escapeString(str : String) = str.replace("\\","\\\\").replace("\n","\\n").replace("\b","\\b").replace("\r","\\r").replace("\t","\\t").replace("\'","\\'").replace("\f","\\f").replace("\"","\\\"")
    
  /**
   * Transform an Observable into a Comet Observable.
   *
   * @tparam E Type of messages handled by this comet stream.
   * @param callback Javascript function to call on the browser for each message.
   * @param initialChunk Initial chunk of data to send for browser compatibility (default to send 5Kb of blank data)
   */
  
  def apply[E](obs: Observable[E], callback: String, initialChunk: Html = Html(Array.fill[Char](5 * 1024)(' ').mkString + "<html><body>")): Observable[Html] = {
    val obsInit: Observable[Html] = Observable.from(List(initialChunk))
    val obsNext: Observable[Html] = obs.map[Html](data => Html("<script type=\"text/javascript\">" + callback + "(\"" + escapeString(data.toString) + "\");</script>"))
    obsInit ++ obsNext
  }
}