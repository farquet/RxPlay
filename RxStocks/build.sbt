import play.Project._

name := "rxStocks"

version := "1.0"

libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "0.17.1"
)

playScalaSettings
