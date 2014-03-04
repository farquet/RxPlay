package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._

object Application extends Controller {

  def index = Action {
    
    val nums: Enumerator[Int] = Enumerator(1, 2, 3, 4, 5, 6)
    val iterPrint : Iteratee[Int, Unit] = Iteratee.foreach(println _)
    val iterSum: Iteratee[Int,Int] = Iteratee.fold[Int,Int](0){ (x, count) => x + count }
    
    /* double input */
    val doubleEnumeratee: Enumeratee[Int, Int] = Enumeratee.map[Int](i => 2 * i)
    val resDouble = nums through doubleEnumeratee
    
    /* filter even numbers */
    val filterEven: Enumeratee[Int, Int] = Enumeratee.filter(i => i % 2 != 0)
    val resFilter = nums through filterEven
    
    /* take two */
    val takeTwo: Enumeratee[Int, Int] = Enumeratee.take(2)
    val resTake = nums through takeTwo
    
    /* skip one */
    val skip: Enumeratee[Int, Int] = Enumeratee.drop(1)
    val resSkip = nums through skip
    
    /* flatMap = map and flatten */
    val nestedNums = Enumerator(Enumerator(1, 2, 3), Enumerator(3, 4, 5))
    
    //resDouble |>> iterPrint
    
    //resFilter |>> iterPrint
   
    //resTake |>> iterPrint
    
    resSkip |>> iterPrint
    
    /* rendering */
    Ok(views.html.index("Your new application is ready !"))
  }

}