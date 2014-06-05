package models

import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.mutable.Map
import rx.lang.scala._
import rx.lang.scala.observables._
import rx.lang.scala.subjects._
import RxPlay._

/* A WidgetManager is used to push data from source Observables to create a websocket to the client.
 * You can to give it two function : processClientData and onClientClose to handle data that the client could give back.
 * It stores all the Observables you give them and can handle subscription to them easily.
 */
class WidgetManager(processClientData: (String, String, WidgetManager) => Unit = {(_,_,_) => }, onClientClose: WidgetManager => Unit = {_ => }) {
	val dataSources: Map[String, Observable[String]] = Map.empty
	val subscriptions: Map[String, Subscription] = Map.empty
	private val output: Subject[String] = Subject()
	
	/**
	 * saving the Observable with its corresponding name
	 */
	def addObservable(name: String, obs:Observable[String]) = dataSources.put(name, obs)
	/**
	 * returns an Option with the Observable of the given name
	 */
	def getObservable(name: String) = dataSources.get(name)
	
	/**
	 * Subscribes to the Observable with the given name and stores its subscription.
	 * Default onNext is called, so every data is pushed to the client with format <obsName>Update:<value>
	 */
	def subscribePush(name: String): Unit = {
	  def onNext: String => Unit = { el => send(name, el.toString) }
	  subscribe(name, onNext)
	}
	
	/**
	 * Subscribes to the Observable with the given name. The onNext method, i.e. the subscription action, is given as parameter.
	 */
	def subscribe(name: String, onNext:String => Unit): Unit = {
	  dataSources.get(name) match {
	    case Some(source) => subscriptions.put(name, source.subscribe(onNext))
	    case None => println("Warning: Unable to subscribe to "+name+". Source observable doesn't exist.")
	  }
	}
	
	/**
	 * Subscribes to each Observable corresponding to the name in the list with default onNext method (data sent to client)
	 */
	def subscribePush(nameList: List[String]): Unit = nameList.foreach(subscribePush(_))
	
	/**
	 * Subscribes to each Observable corresponding to the name in the list with the given onNext method for subscription
	 */
	def subscribe(nameList: List[String], onNext:String => Unit): Unit = nameList.foreach(subscribe(_, onNext))
	
	/**
	 * Unsubscribes from the Observable with the given name
	 */
	def unsubscribe(name: String): Unit = subscriptions.get(name) match {
	  case Some(subscription) => subscription.unsubscribe
	  case None => println("Warning: Unable to unsubscribe from "+name+". It has never been subscribed to.")
	}
	/**
	 * Unsubscribes from all given Observables
	 */
	def unsubscribe(nameList: List[String]): Unit = nameList.foreach(unsubscribe(_))
	/**
	 * Unsubscribes from all Observables of the WidgetManager
	 */
	def unsubscribeAll: Unit = subscriptions.keys.foreach(unsubscribe(_))
	
	/**
	 * Push a command to the client with the format <command>:<data>
	 */
    def send(command:String, data:String) = output.onNext(command+"Update:"+data)
    
    def close: Unit = output.onCompleted
	
    /**
     * Returns directly the WebSocket that Play expects.
     */
	def webSocket: (Iteratee[String, _], Enumerator[String]) = {
	  val in = Iteratee.foreach[String] { data =>
	    val (func, arg) = splitFuncArg(data)
	    processClientData(func, arg, this)
	  }
	  
	  (in, output)
	}
	
	/**
	 * Converts the string "func:args" to (func, args)
	 */
	private def splitFuncArg(msg: String): (String, String) = { // cut string at first : occurence
    val index = msg.indexOf(":")
    val func = msg.split(":").head
    
    if (index < 0) { // client sent <func> to the server with no arg
      (func, "")
    } else {
      (func, msg.substring(index+1))
    }
  }
}