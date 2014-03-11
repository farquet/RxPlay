/*

Conversion from Observables to Iteratees and from Iteratees to Observables.
Solution from Byran Gilbert (http://bryangilbert.com)

*/


import rx.lang.scala._
import scala.concurrent.duration._
import scala.concurrent._
import scala.util._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import java.io.File
import Enumerator.Pushee
import ExecutionContext.Implicits.global

import RxPlay._


object Test extends App {
  /* Running fold iteratee on observer */
  val observer = Observable(1, 2, 3, 4, 5)
  val res = observer.run(Iteratee.fold(0) { (total, elt) => total + elt })
  println(Await.result(res, 5 seconds))

  /* Using Enumerator created from file as an observer */
  val fileEnum = Enumerator.fromFile(new File("test.txt"), 1)
  val fileObs: Observable[Array[Byte]] = fileEnum
  fileObs.map(new String(_)).subscribe(println(_))

  // Using implicit conversion to apply composed enumeratees and iteratee to async observable 
  val filterOdd = Enumeratee.filter[Long](_ % 2 != 0)
  val takeFive = Enumeratee.take[Long](5)
  val intToString = Enumeratee.map[Long](_.toString)
  val composed = filterOdd compose takeFive compose intToString

  val asyncIntObserverable = Observable.interval(50 millis)
  asyncIntObserverable through composed run Iteratee.foreach(println(_))
  
}