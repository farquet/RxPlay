package controllers

import play.api._
import play.api.mvc._

import play.api.libs.Comet
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import java.util.Date
import java.text._

import scala.concurrent.Future
import rx.lang.scala._

import RxPlay._

object Application extends Controller {
  
  /** 
   * A String Enumerator producing a formatted Time message every 100 millis.
   * A callback enumerator is pure an can be applied on several Iteratee.
   */
  
  /*lazy val clock: Enumerator[String] = {
    
    val dateFormat = new SimpleDateFormat("HH mm ss")
    
    Enumerator.generateM {
      Promise.timeout(Some(dateFormat.format(new Date)), 100 milliseconds)
    }
  }*/

  /* with observables */
  
  val obs = Observable.interval(100 milliseconds).map(_ => new SimpleDateFormat("HH mm ss").format(new Date))
  
  def liveClock = Action {
    Ok.chunked(observable2Enumerator(obs) &> Comet(callback = "parent.clockChanged"))
  }
  
  def index = Action {
    Ok(views.html.index())
  }
}
