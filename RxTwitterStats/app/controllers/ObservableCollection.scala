package controllers

import play.api._
import play.api.mvc._
import rx.lang.scala.Observable

class ObservableCollection[T] {
	private var obsList: List[Observable[T]]=Nil
	
	def addObservable(obs: Observable[T]): Unit = {
	  obsList = obsList ::: obs :: Nil
	}
	
	def getObservable(index: Int): Observable[T] = {
	  if (index < 0 || index >= obsList.length) null
	  else obsList.drop(index).head
	}
	
	def removeObservable(index: Int): Boolean = {
	  val length = obsList.length
	  obsList = obsList.zipWithIndex.filter(_._2 != index).map(_._1)
	  length != obsList.length
	}
	
	def toList() : List[Observable[T]] = {
	  val l = obsList
	  l // returning a read-only version of that list
	}
}