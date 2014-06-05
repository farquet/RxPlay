package controllers

import play.api._
import play.api.mvc._
import play.Configuration
import play.Play
import play.api.libs.iteratee._
import play.api.templates._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
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
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http._
import org.apache.http.client.methods.HttpGet
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.commons.io.IOUtils

import models.RxPlay._
import models.WidgetManager

object Application extends Controller {
  
  /**
   * For a specific Twitter url of the streaming API,
   * will return the status code sent by Twitter and the corresponding input stream
   */
  def twitterRequest(req: String): (Int, InputStream) = { // return HTTP status and inputStream
    
     // Retrieving secret tokens and keys in passwords.conf
     val cfg = Play.application.configuration
     val AccessToken = cfg.getString("twitter.accessToken")
     val AccessSecret = cfg.getString("twitter.accessSecret")
     val ConsumerKey = cfg.getString("twitter.consumerKey")
     val ConsumerSecret = cfg.getString("twitter.consumerSecret")
    
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
   * Converts an inputStream to an Observable of Strings
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
	       //println(tweet.take(180)+" ...")
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
    
    val req = "https://stream.twitter.com/1.1/statuses/filter.json?track="+URLEncoder.encode(keyword, "UTF-8")+"&filter_level=none&stall_warnings=true"
    val (status, is) = twitterRequest(req)
    
    if (status != 200) {
      println("Bad status from Twitter : "+status)
      None
    } else {
      Some(observableFromStream(is))
    }
  }

  /**
   * Creates a WebSocket
   * 'in' is the consumer of data received from the client
   * 'messages' is the data sent to the client
   */
  def socket = WebSocket.using[String] { request =>
  
  val submit = Subject[Observable[String]]()
  val tweets = (submit.switch).publish // switching to the most recent twitter feed
  
  // data to send between twitter feed to know that we are changing keyword
  def resetObs = Observable.from(List("RESET"))
  
  def processClientData(func: String, arg:String, m:WidgetManager) = {
    func match {
        case "stop" =>
          submit.onNext(resetObs) // to notify subscribers that we stop
          m.send("twitter", "Enter a keyword to receive twitter updates.")
         
        case "pause" =>
          submit.onNext(Observable.empty) // to stop receiving tweets
        
        case "resumeKeyword" if (arg.length > 0) =>
          twitterFeedKeyword(arg) match {
            case Some(obs) => submit.onNext(obs) // adding the feed to the Subject of Observable
            case None => m.send("twitter", "Error recovering the Twitter feed...")
          }
        
        case "keywordChanged" if (arg.length > 0) =>
          submit.onNext(resetObs) // to notify subscribers that we change twitter stream
          twitterFeedKeyword(arg) match {
            case Some(obs) => {
              m.send("twitter", "Awaiting tweets for keyword : "+xml.Utility.escape(arg))
              submit.onNext(obs) // adding the feed to the Subject of Observable
            }
            case None => m.send("twitter", "Error setting the Twitter feed...")
          }
        case _ => 
          println("Unrecognized input <"+func+":"+arg+">")
    }
  }
  
  // cleaning the app
  def onClientClose(m: WidgetManager) = {
    println("Closing all connections.")
    
    // stopping consumers
    m.unsubscribeAll
	
	// stopping producers
	submit.onNext(Observable.empty)
	submit.onCompleted
	
	// closing manager
	m.close
  }
  
  val manager = new WidgetManager(processClientData, onClientClose)
  
  val ctrObs = tweets.scan(0)((ctr, tweet) => if (tweet == "RESET") 0 else ctr + 1).map(_.toString)
  val speedObs = tweets.buffer(1 second).filter(_.length > 0).filter(_ != "RESET").map(_.length.toString)
  val top3Obs = tweets.scan(Map.empty[String,Int])((m, t) => {
    if (t == "RESET") {
        Map.empty // cleaning the mentions ranking if we change feed
     } else {
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
        m // returning the updated map
      }
    }).map({ map =>
    val top3 = map.toList.sortBy(-_._2).take(3)
    top3.map(el => el._1+","+el._2).mkString(";")
  }).buffer(100 milliseconds).filter(_.length > 0).map(_.head)
  
  // sends tweet text to client extracted from json  	  
  manager.addObservable("tweets", tweets.buffer(100 milliseconds).filter(_.length > 0).map(_.head))
  manager.subscribe("tweets", { jsonTweet:String =>
    try {
  	    if (jsonTweet != "RESET") {
  	    val text = (Json.parse(jsonTweet) \ "text").toString
  	    if (text.length > 2)
  	      manager.send("twitter", text.substring(1, text.length-1))
  	    }
      } catch {
  	    case e: JsResultException => println("Unable to parse : "+jsonTweet)
  	  }
  })
  
  tweets.connect // to sync Observables subscribed to the same feed
  
  // real-time tweet counter sent to browser
  manager.addObservable("counter", ctrObs)  
  manager.subscribePush("counter")
  
  // sends the number of tweets per second to the client
  manager.addObservable("speed", speedObs)
  manager.subscribePush("speed")
  
  // sends top 3 mentions until now
  manager.addObservable("mentionsRank", top3Obs)
  manager.subscribePush("mentionsRank")
  
  manager.webSocket
  }
  
  def index = Action {
    Ok(views.html.index())
  }

}
