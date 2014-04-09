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
 
  var obsCollection : Map[String, Observable[String]] = Map.empty
 
  
  def twitterRequest(req: String): Observable[String] = {
   
  Observable({ obs: Observer[String] =>
     // setting keys and tokens in the request
	 val consumer = new CommonsHttpOAuthConsumer(ConsumerKey,ConsumerSecret)
	 consumer.setTokenWithSecret(AccessToken, AccessSecret)
     
     val request = new HttpGet(req)
     consumer.sign(request) // signing the current request with the authentication set above
     
     val client = new DefaultHttpClient()
     val response = client.execute(request)
     val status = response.getStatusLine().getStatusCode()
     val inputStream = response.getEntity().getContent()
     val buffer = Source.fromInputStream(inputStream).bufferedReader
     
     println("Status code : "+status)
     
     try {
       var stop = false
       do {
         val tweet = buffer.readLine() // one tweet has been pushed by Twitter to us
         System.out.println(tweet)
         if (tweet.length > 0) // skipping keep alive messages
        	 obs.onNext(tweet)
         if (tweet == null) stop = true // we stop looping if we reached the end of the stream
       } while(!stop)
       obs.onCompleted // Twitter has closed the connection without error
     } catch {
       case e : Throwable => {
         obs.onError(e) // passing the error to the Observable
         System.err.println("Error : " + e)
       }
     }
     
    })
  }
  
  def rxTwitterStats = Action {
    
    // Twitter streaming API : https://dev.twitter.com/docs/streaming-apis/parameters
    val keyword = "NBA"
    val req = "https://stream.twitter.com/1.1/statuses/filter.json?track="+keyword+"&filter_level=none&stall_warnings=true"
    
    val obs = twitterRequest(req)
    val textObs = obs.map(data => (Json.parse(data.mkString) \ "text").toString).filter(_.length > 0)
    
    Ok.chunked(CometObs(textObs, callback = "parent.twitterUpdate"))
  }
  
  def changeKeyword(keyword: String) = Action {
    // TODO
    Ok(views.html.index())
  }
  
  def index = Action {
    Ok(views.html.index())
  }
}
