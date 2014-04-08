package controllers

import play.api.templates.Html
import rx.lang.scala.Observable

object CometObs {

  /**
   * Transform an Observable into a Comet Observable.
   *
   * @tparam E Type of messages handled by this comet stream.
   * @param callback Javascript function to call on the browser for each message.
   * @param initialChunk Initial chunk of data to send for browser compatibility (default to send 5Kb of blank data)
   */
  
  def apply[E](obs: Observable[E], callback: String, initialChunk: Html = Html(Array.fill[Char](5 * 1024)(' ').mkString + "<html><body>")): Observable[Html] = {
    val obsInit: Observable[Html] = Observable.from(List(initialChunk))
    val obsNext: Observable[Html] = obs.map[Html](data => Html("<script type=\"text/javascript\">" + callback + "(\"" + escapeString(data.toString) + "\");</script>"))
    obsInit ++ obsNext
  }
  
  private def escapeString(str : String) = str.replace("\\","\\\\").replace("\n","\\n").replace("\b","\\b").replace("\r","\\r").replace("\t","\\t").replace("\'","\\'").replace("\f","\\f").replace("\"","\\\"")
}