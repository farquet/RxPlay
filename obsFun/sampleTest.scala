import rx.lang.scala._
import rx.lang.scala.schedulers._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.language.implicitConversions


object SampleTest extends App {
  
  // observable handling manually the subscriber
  def obs1 : Observable[Int] = Observable { sub:Subscriber[Int] =>
    var i = 0
    while(!sub.isUnsubscribed) {
      Thread.sleep(1000)
      sub.onNext{ i }
      i += 1
    }

    if(sub.isUnsubscribed){ println("Unsubscribed !"); sub.onCompleted() }
    
  } finallyDo { () => println("Finally 2") }
  
  // observable using api
  def obs2: Observable[Int] = Observable.interval(1 second).map(_.toInt).finallyDo { () => println("Finally 2") } 
  
  // emitting new infinite observable every 5 seconds
  def obsception: Observable[Observable[Int]] = Observable.interval(5 seconds).map(el => obs2)
 
  (obsception.switch).subscribe(el => println(el))
  Thread.sleep(10000)
}
