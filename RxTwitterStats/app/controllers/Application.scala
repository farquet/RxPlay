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
import java.net.URLEncoder

import scala.io.Source
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.collection.mutable.Map
import rx.lang.scala._
import rx.lang.scala.subjects._

import RxPlay._

import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.commons.io.IOUtils


object Application extends Controller {
  
  // tokens generated on https://dev.twitter.com/apps/
  
  val AccessToken = "314556020-gjT0EUkAQ3jFRcLu0ooQ9e2eYFCDKY544BgvDBUS"
  val AccessSecret = "xxdc7FFSwddvXP1BGFxSlHcA6OKZkqxvNLgedaMu2WrSW"
  val ConsumerKey = "jv8ZWJsb8X7DJKKirEGOCknuI"
  val ConsumerSecret = "0Dx3Ogo7x64bZkpLiILQSveUR9jPgf5cXWdx47eBXFGLv7Xy5V"
 
  var switch = false
  
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
     
     val client = new DefaultHttpClient()
     val response = client.execute(request)
     val status = response.getStatusLine().getStatusCode()
     val inputStream = response.getEntity().getContent()
     
     (status, inputStream)
  }
  
  /**
   * Given an inputStream, it will convert the data received in a Subject of String
   * One string returned contains one line of the stream read
   */
    def observableFromStream(is: InputStream): Observable[String] = {
    
    val bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))
    
    Observable { obs: Observer[String] =>
	  
	 try {
	   val simpleIterator: Observable[Int] = Observable { subscriber =>
    var i = 0
    while(!subscriber.isUnsubscribed) {
      subscriber.onNext{ println("whappen deh: "+ i); i }
      i += 1
    }

    if(subscriber.isUnsubscribed){ subscriber.onCompleted() }
  }
	 } catch {
       case e : Throwable => {
         obs.onError(e) // passing the error to the Observable
         System.err.println("Error : " + e.getMessage())
       }
     }
	 
	 // Closing connection
	 new Subscription { override def unsubscribe() = {
	   println("Someone unsubscribed")
	   obs.onCompleted
	   bufferedReader.close() // TODO maybe a nicer way to close without throwing
	 }
	 }
    } finallyDo {
      println("CLOSING STREAM")
      bufferedReader.close
    }
  }
   
  /**
   * old version using a future that will call himself
   */
  def observableFromStream2(is: InputStream): Observable[String] = {
    
    val bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))
    
    Observable { obs: Observer[String] =>
	  
	 try {
	   def getTweet:Unit = {
	     val f = Future[String] {
	       val tweet = bufferedReader.readLine
	       println(tweet.take(180)+" ...")
	       tweet
	     }
	     f onComplete {
	       case Success(s) => {
	         obs.onNext(s) // adding the received value to the Observable
	    	 getTweet // fetching the next value
	       }
	       case Failure(e) => {
	         println("Error with Twitter stream :"+e.getMessage())
	         obs.onNext("twitterUpdate:\"Network error : "+e+"\"") // propagating error
	       }
	     }
	   }
	   getTweet
	   
	 } catch {
       case e : Throwable => {
         obs.onError(e) // passing the error to the Observable
         System.err.println("Error : " + e.getMessage())
       }
     }
	 
	 // Closing connection
	 new Subscription { override def unsubscribe() = {
	   println("Someone unsubscribed")
	   obs.onCompleted
	   bufferedReader.close() // TODO maybe a nicer way to close without throwing
	 }
	 }
    } finallyDo {
      println("CLOSING STREAM")
      bufferedReader.close
    }
  }
  
  /**
   * Returns an Observable of JSON tweets as Strings for a given keyword
   */
  def twitterFeedKeyword(keyword: String): Observable[String] = {
    // Twitter streaming API : https://dev.twitter.com/docs/streaming-apis/parameters
    
    println("Setting keyword to : "+keyword)
    
    val req = "https://stream.twitter.com/1.1/statuses/filter.json?track="+keyword+"&filter_level=none&stall_warnings=true"
    val (status, is) = twitterRequest(req)
    
    observableFromStream(is)
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
   * React to data sent from the client (pretty much like an Actor)
   */
  def clientHasSent(msg: String) = {
    val (func, arg) = splitFuncArg(msg)
    
    if (arg == null || arg.length <= 0) { // client sent <func> to the server. We react to it
      println("received <"+func+">")
      func match {
        case "close" => // client force the server to close connection
          println("User want to stop receiving data.")
          // TODO stop getting data from Twitter
        case o => // 
          println("Client sent "+o+" (unknown function)")
      }
    } else {
      println("received <"+func+"> with arg <"+arg+">")
      func match {
        case "keywordChanged" => { // client wants
          //val req = "https://stream.twitter.com/1.1/statuses/filter.json?track="+URLEncoder.encode(arg, "UTF-8")+"&filter_level=none&stall_warnings=true"
          //val (status, is) = twitterRequest(req)
          // TODO use this new input stream 'is' in replacement of the old one
        }
      }
    }
  }
  
  /**
   * Creates a WebSocket
   * 'in' is the consumer of data received from the client
   * 'textObs' is the data sent to the client
   */
  def socket = WebSocket.using[String] { request =>
  
  val submit = Subject[Subject[String]]()
  val messages = Subject[String]()
  
  val in = Iteratee.foreach[String](dataReceived => {
    
    if (dataReceived == Enumerator.eof) {
      println("Client has closed the stream")
    } else {
      println(dataReceived)
        
      val (func, arg) = splitFuncArg(dataReceived)
      
      func match {
        case "ping" =>
          println("Handling ping")
          messages.onNext("twitterUpdate:\"pong !\"")
          
        case "stop" =>
          println("Handling stop")
          //a.unsubscribe
          
        case "keywordChanged" if (arg.length >= 2) =>
          println("Handling keyword change")
          messages.onNext("twitterUpdate:\"Changing keyword to "+arg+"\"")
          
          val feed = Subject[String]() // creating a new Subject to fill with tweets
          twitterFeedKeyword(arg).subscribe(el => feed.onNext("twitterUpdate:"+(Json.parse(el) \ "text").toString))
          submit.onNext(feed) // adding the feed to the switched Subject
      }
      
    }
  })
  
  // submit.switch allow changing data feed
  val tweetFeed = submit.switch
  
  val res = tweetFeed.merge(messages)
  
  (in, res)
}
  
  def index = Action {
    Ok(views.html.index())
  }
  
  /*--------------------------------------*/
  /*| For Comet use instead of WebSocket |*/
  /*--------------------------------------*/
  
  // mutable map between the name of the javascript function to call on the client side and the corresponding feed of data as an Observable
  val obsCollection : Map[String, Observable[String]] = Map.empty
  
  def changeKeyword(keyword: String) = Action {
    val jsonObs = twitterFeedKeyword(keyword)
    val textObs = jsonObs.map(data => (Json.parse(data.mkString) \ "text").toString).filter(_.length > 0)
    
    // TODO will not work because obsCollection sent to CometObs has already copied references to old key/value pair
    obsCollection.update("parent.twitterUpdate", textObs)
    
    Ok("Ok")
  }
  
  def rxTwitterStats = Action {
    val keyword = "NBA"
    val jsonObs = twitterFeedKeyword(keyword)
    
    val textObs = jsonObs.map(data => (Json.parse(data.mkString) \ "text").toString).filter(_.length > 0)
    
    obsCollection.+=(("parent.twitterUpdate", textObs))
    
    Ok.chunked(CometObs(obsCollection))
  }
  
  def cometIndex = Action {
    Ok(views.html.cometIndex())
  }
}
