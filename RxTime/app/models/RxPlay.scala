/*
Conversion from Observables to Iteratees and from Iteratees to Observables.
Solution from Bryan Gilbert (http://bryangilbert.com)
*/

package models

import rx.lang.scala._
import scala.concurrent._
import scala.util._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import ExecutionContext.Implicits.global


object RxPlay {
  /*
   * Enumerator to Observable
   */
  implicit def enumerator2Observable[T](enum: Enumerator[T]): Observable[T] = {
    // creating the Observable that we return
    Observable({ observer: Observer[T] =>
      // keeping a way to unsubscribe from the observable
      var cancelled = false

      // enumerator input is tested with this predicate
      // once cancelled is set to true, the enumerator will stop producing data
      val cancellableEnum = enum through Enumeratee.breakE[T](_ => cancelled)
      
      // applying iteratee on producer, passing data to the observable
      cancellableEnum (
        Iteratee.foreach(observer.onNext(_))
      ).onComplete { // passing completion or error to the observable
        case Success(_) => observer.onCompleted()
        case Failure(e) => observer.onError(e)
      }

      // unsubscription will change the var to stop the enumerator above via the breakE function
      new Subscription { override def unsubscribe() = { cancelled = true } }
    })
  }

  /*
   * Observable to Enumerator
   */
  implicit def observable2Enumerator[T](obs: Observable[T]): Enumerator[T] = {
    // unicast create a channel where you can push data and returns an Enumerator
    Concurrent.unicast { channel =>
      val subscription = obs.subscribe(new ChannelObserver(channel))
      val onComplete = { () => subscription.unsubscribe }
      val onError = { (_: String, _: Input[T]) => subscription.unsubscribe }
      (onComplete, onError)
    }
  }
  
  // the observer is a mechanism that encapsulates onNext, onCompleted and onError
  // this will be used by the observable as callback methods if the observer is given as subscription
  class ChannelObserver[T](channel: Channel[T]) extends rx.lang.scala.Observer[T] {
    override def onNext(elem: T): Unit = channel.push(elem)
    override def onCompleted(): Unit = channel.end()
    override def onError(e: Throwable): Unit = channel.end(e)
  }
  
}