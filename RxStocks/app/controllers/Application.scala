package controllers

import play.api._
import play.api.mvc._

import play.api.libs.iteratee._
import play.api.templates._
import play.api.libs.json._

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import java.util.Date
import java.text._

import scala.io.Source
import scala.concurrent.Future
import scala.util.{Success, Failure}
import rx.lang.scala._

import RxPlay._

object Application extends Controller {
  
  def getStockValue(symbol: String): String = {
	val data = Source.fromURL("http://www.google.com/finance/info?q="+symbol)
	val json: JsValue = Json.parse(data.mkString drop 3) // droping "// " at the beginning
	(json \\ "l_cur").headOption.get.toString
  }
  
  val obs: Observable[String] = Observable.interval(5 seconds).map(_ => getStockValue("LOGN"))
  
  def liveStock = Action {
    Ok.chunked(CometObs(obs, callback = "parent.stockChanged"))
  }
  
  def index = Action {
    Ok(views.html.index())
  }
}
