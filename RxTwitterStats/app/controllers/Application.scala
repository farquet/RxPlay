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
import java.io._
import java.net.{URI, URLDecoder, URLEncoder}

import scala.io.Source
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.collection.mutable.Map
import rx.lang.scala._
import rx.lang.scala.subjects._

import RxPlay._

import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http._
import org.apache.http.client.methods.HttpGet
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.commons.io.IOUtils


object Application extends Controller {
  
  // tokens generated on https://dev.twitter.com/apps/
  
  val AccessToken = "314556020-gjT0EUkAQ3jFRcLu0ooQ9e2eYFCDKY544BgvDBUS"
  val AccessSecret = "xxdc7FFSwddvXP1BGFxSlHcA6OKZkqxvNLgedaMu2WrSW"
  val ConsumerKey = "jv8ZWJsb8X7DJKKirEGOCknuI"
  val ConsumerSecret = "0Dx3Ogo7x64bZkpLiILQSveUR9jPgf5cXWdx47eBXFGLv7Xy5V"
 
  /**
   * For a specific Twitter url of the streaming API,
   * will return the status code sent by Twitter and the corresponding input stream
   */
  def twitterRequest(req: String): (Int, InputStream) = { // return HTTP status and inputStream
     // setting keys and tokens in the request
	 val consumer = new CommonsHttpOAuthConsumer(ConsumerKey,ConsumerSecret)
	 consumer.setTokenWithSecret(AccessToken, AccessSecret)
     
     val request = new HttpGet(req)
     consumer.sign(request) // signing the current request with the authentication set above
     
     val client = new DefaultHttpClient
	 println("Opening new Twitter stream.")
     val response = client.execute(request)
     val status = response.getStatusLine.getStatusCode
     val inputStream = response.getEntity.getContent
     
     (status, inputStream)
  }
  
  /**
   * old version using a future that will call himself
   */
  def observableFromStream(is: InputStream): Observable[String] = {
    
    val bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))
    
    def closeReader: Unit = {
      println("App is closing Twitter stream.")
      
      if (bufferedReader.ready)
    	  bufferedReader.close
    }
    
    Observable { sub: Subscriber[String] =>
	  
	 try {
	   def getTweet:Unit = {
	     val f = Future[String] {
	       val tweet = bufferedReader.readLine
	       println(tweet.take(180)+" ...")
	       tweet
	     }
	     f onComplete {
	       case Success(s) => {
	         if(!sub.isUnsubscribed) {
	           if (s.length > 0)
	        	   sub.onNext(s) // adding the received value to the Observable
	           getTweet // fetching the next value
	         } else {
	           println("Unsubscribed from Twitter stream.")
	           closeReader
	         }
	       }
	       case Failure(e) => {
	         println("Error with Twitter stream : "+e.getMessage())
	         sub.onError(e) // propagating the error
	         closeReader
	       }
	     }
	   }
	   getTweet
	   
	 } catch {
       case e : Throwable => {
         sub.onError(e) // passing the error to the Observable
         System.err.println("Error : " + e.getMessage())
       }
     }
    }.filter(_.length > 0) // deleting keep-alive data from Twitter
  }
  
  /**
   * Returns an Observable of JSON tweets as Strings for a given keyword
   */
  def twitterFeedKeyword(keyword: String): Option[Observable[String]] = {
    // Twitter streaming API : https://dev.twitter.com/docs/streaming-apis/parameters
    
    println("Setting keyword to : "+keyword)
    
    val req = "https://stream.twitter.com/1.1/statuses/filter.json?track="+URLEncoder.encode(keyword,"UTF-8")+"&filter_level=none&stall_warnings=true"
    val (status, is) = twitterRequest(req)
    
    if (status != 200) {
      println("Bad status from Twitter : "+status)
      None
    } else {
      Some(observableFromStream(is))
    }
  }
  
  private def splitFuncArg(msg: String): (String, String) = { // cut string at first : occurence
    val index = msg.indexOf(":")
    val func = msg.split(":").head
    
    if (index < 0) { // client sent <func> to the server with no arg
      (func, "")
    } else {
      (func, msg.substring(index+1))
    }
  }

  /**
   * Creates a WebSocket
   * 'in' is the consumer of data received from the client
   * 'messages' is the data sent to the client
   */
  def socket = WebSocket.using[String] { request =>
  
  val submit = Subject[Observable[String]]()
  val messages = Subject[String]()
  val tweets = (submit.switch).publish // switching to the most recent twitter feed
  
  // helper methods to push a command to the browser
  def sendInt(command:String, data:Int): Unit = send(command, data.toString)
  def send(command:String, data:String) = messages.onNext(command+"Update:"+data)
  
  // pushing tweet to the tweetbox in the browser
  val tweetSub:Subscription = tweets.subscribe { el =>
  	    try {
  	      val text = (Json.parse(el) \ "text").toString
  	      if (text.length > 2)
  	    	  send("twitter", text.substring(1, text.length-1))
        } catch {
  	      case e: JsResultException => println("Unable to parse : "+el)
  	    }
  	 }
  
  // real-time tweet counter sent to browser
  val ctrSub:Subscription = tweets.scan(0)((ctr, tweet) => ctr + 1).subscribe { c => sendInt("counter", c) }
  
  // sends the number of tweets per second to the client
  val speedSub:Subscription = tweets.buffer(1 second).filter(_.length > 0).subscribe { el => sendInt("speed", el.length) }
  
  // sends top 3 mentions until here
  val mentionSub:Subscription = tweets.scan(Map.empty[String,Int])((m, t) => {
     try {
      val mentions = (Json.parse(t) \ "entities" \ "user_mentions")
      
      mentions.as[List[JsValue]].foreach { user =>
          val name = (user \ "screen_name").toString
          m.update(name, m.getOrElse(name, 0) + 1)
        }
    } catch {
      case e: JsResultException => {
        println("Unable to parse : "+t)
      }
    }
    m
  }).subscribe { map =>
    // TODO map.toList
    val top3 = map.foldLeft(List[(String, Int)]()) { (res, user) => { (user :: res).sortBy(_._2).reverse.take(3) } }
    top3.foreach(el => println(el._1+" : "+el._2))
  }
  
  // cleaning the app
  def clientClose = {
    println("Closing all connections.")
    
    // stopping consumers
    if (!tweetSub.isUnsubscribed) tweetSub.unsubscribe
	if (!ctrSub.isUnsubscribed) ctrSub.unsubscribe
	if (!mentionSub.isUnsubscribed) mentionSub.unsubscribe
	
	// stopping producers
	submit.onNext(Observable.empty)
	submit.onCompleted
	messages.onCompleted
  }
  
  val in = Iteratee.foreach[String](dataReceived => {
     
      println(dataReceived)
      
      val (func, arg) = splitFuncArg(dataReceived)
      
      func match {   
        case "stop" =>
          println("Handling stop")
          submit.onNext(Observable.never) // to close last Twitter stream and wait for user action
          send("twitter", "Enter a keyword to receive twitter updates.")
        
        case "keywordChanged" if (arg.length > 0) =>
          println("Handling keyword change")
          twitterFeedKeyword(arg) match {
            case Some(obs) => submit.onNext(obs) // adding the feed to the Subject of Observable
            case None => send("twitter", "Error setting the Twitter feed...")
          }
        case _ =>
          println("Unrecognized input <"+dataReceived+">")
      }
    }).map(_ => clientClose) // closing connection when finished consuming data
  
  tweets.connect // to sync Observables subscribed to the same feed
  
  (in, messages)
}
  
  def index = Action {
    Ok(views.html.index())
  }
  
// unused for the moment but this could be a good way to handle data instead of giving the three functions
class StreamObserver[Int](reader: BufferedReader) extends rx.lang.scala.Observer[Int] {
  override def onNext(elem: Int): Unit = { println("Subscriber received : "+elem) }
  override def onCompleted(): Unit = { () => println("completed !") }
  override def onError(e: Throwable): Unit = { println("error :"+e.getMessage) }
}
  
  /*--------------------------------------*/
  /*| For Comet use instead of WebSocket |*/
  /*--------------------------------------*/
  
  // mutable map between the name of the javascript function to call on the client side and the corresponding feed of data as an Observable
  val obsCollection : Map[String, Observable[String]] = Map.empty
  
  def changeKeyword(keyword: String) = Action {
    val jsonObs = twitterFeedKeyword(keyword).getOrElse(Observable.empty)
    
    val textObs = jsonObs.map(data => (Json.parse(data.mkString) \ "text").toString).filter(_.length > 0)
    
    
    // TODO will not work because obsCollection sent to CometObs has already copied references to old key/value pair
    obsCollection.update("parent.twitterUpdate", textObs)
    
    Ok("Ok")
  }
  
  def rxTwitterStats = Action {
    val keyword = "NBA"
    val jsonObs = twitterFeedKeyword(keyword).getOrElse(Observable.empty)
    
    val textObs = jsonObs.map(data => (Json.parse(data.mkString) \ "text").toString).filter(_.length > 0)
    
    obsCollection.+=(("parent.twitterUpdate", textObs))
    
    Ok.chunked(CometObs(obsCollection))
  }
  
  def cometIndex = Action {
    Ok(views.html.cometIndex())
  }
}
