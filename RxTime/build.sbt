import play.Project._

name := "RxTime"

version := "1.0"

libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "0.17.4"
)

playScalaSettings
