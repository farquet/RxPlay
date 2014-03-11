import rx.lang.scala._

object ObservablesDemo {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(76); 
  
  println("hello world");$skip(51); 
  
  val intObservable = Observable(1, 2, 3, 4, 5);System.out.println("""intObservable  : rx.lang.scala.Observable[Int] = """ + $show(intObservable ))}
}
