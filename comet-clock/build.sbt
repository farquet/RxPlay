import play.Project._

name := "comet-clock"

version := "1.0"

libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "0.14.6"
)

playScalaSettings
