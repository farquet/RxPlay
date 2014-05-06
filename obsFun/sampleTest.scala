import rx.lang.scala._
import rx.lang.scala.schedulers._

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.language.implicitConversions
import ExecutionContext.Implicits.global

import scala.util.{Success, Failure}
import java.io.BufferedReader

object SampleTest extends App {
  
  var obsNum = 0
  def obs: Observable[Int] = Observable { sub:Subscriber[Int] =>
    
    val id = obsNum
    obsNum = obsNum + 1
    
    try {
       var i = 0
	   def iter:Unit = {
	     val f = Future[Int] {
	       Thread.sleep(500)
	       println("Observable "+id+" produces : "+i)
	       i = i+1
	       i
	     }
	     f onComplete {
	       case Success(s) => {
	         sub.onNext(s) // adding the received value to the Observable
	    	 if (!sub.isUnsubscribed) {
	    		 iter // fetching the next value
	    	 } else {
	    	   println("Someone unsubscribed from Observable "+id+" !")
	    	 }
	       }
	       case Failure(e) => {
	         println("Error on Observable "+id+" :"+e.getMessage())
	       }
	     }
	   }
	   iter
    } catch {
       case e : Throwable => {
         sub.onError(e) // passing the error to the Observable
         System.err.println("Error on Observable "+id+" : " + e.getMessage())
       }
     }
    if(sub.isUnsubscribed){ println("Unsubscribed !"); sub.onCompleted() }
  }
  
  
  // emitting new infinite observable every 5 seconds
  def obsception: Observable[Observable[Int]] = Observable.interval(5 seconds).map(el => obs)
  
  // subscribing with a observer that encapsulates onNext, onError and onCompleted
  val subscription = (obsception.switch).apply(new StreamObserver())
  
  Thread.sleep(20000) // because BlockingObservable doesn't have the same methods as Observable
  subscription.unsubscribe
  Thread.sleep(5000)
}

// embedding onNext, onComplete and onError in an Observer
class StreamObserver[Int] extends rx.lang.scala.Observer[Int] {
  override def onNext(elem: Int): Unit = { println("Subscriber received : "+elem) }
  override def onCompleted(): Unit = { () => println("completed !") }
  override def onError(e: Throwable): Unit = { println("error :"+e.getMessage) }
}
