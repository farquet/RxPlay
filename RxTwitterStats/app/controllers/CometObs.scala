package controllers

import play.api.templates.Html
import rx.lang.scala.Observable
import rx.lang.scala._

import ImplicitFunctionConversions._
import JavaConversions._

object CometObs {

  /**
   * Transform an Observable into a Comet Observable.
   *
   * @tparam E Type of messages handled by this comet stream.
   * @param callback Javascript function to call on the browser for each message.
   * @param initialChunk Initial chunk of data to send for browser compatibility (default to send 5Kb of blank data)
   */
  
  def apply[E](obs: Observable[E], callback: String, initialChunk: Html = Html(Array.fill[Char](5 * 1024)(' ').mkString + "<html><body>")): Observable[Html] = {
    
    val obsNext: Observable[Html] = convertToJsFunction(obs, callback)
    
    if (initialChunk != null) {
      val obsInit: Observable[Html] = Observable.from(List(initialChunk))
      obsInit ++ obsNext
    }
    else
      obsNext
  }
  
    def apply[E](obsMap: Map[String, Observable[E]], initialChunk: Html = Html(Array.fill[Char](5 * 1024)(' ').mkString + "<html><body>")): Observable[Html] = {
    
    val obsInit: Observable[Html] = Observable.from(List(initialChunk))
    
    val allObsList = obsMap.values.toList
    val callbackList = obsMap.keys.toList
    
    val htmlObsList = allObsList.zip(callbackList).map(el => convertToJsFunction(el._1, el._2))
    
    obsInit ++ htmlObsList.head // TODO mergeDelayError all list
    //obsInit ++ Observable.mergeDelayError(htmlObsList)
  }
  
  private def convertToJsFunction[E](obs: Observable[E], callback: String): Observable[Html] = {
    obs.map[Html](data => Html("<script type=\"text/javascript\">" + callback + "(\"" + escapeString(data.toString) + "\");</script>"))
  }
  
  private def escapeString(str : String) = str.replace("\\","\\\\").replace("\n","\\n").replace("\b","\\b").replace("\r","\\r").replace("\t","\\t").replace("\'","\\'").replace("\f","\\f").replace("\"","\\\"")
}