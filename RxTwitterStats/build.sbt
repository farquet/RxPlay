import play.Project._

name := "RxTwitterStats"

version := "1.0"

libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "0.17.4",
  "oauth.signpost" % "signpost-core" % "1.2.1.2",
  "oauth.signpost" % "signpost-commonshttp4" % "1.2.1.2",
  "org.apache.httpcomponents" % "httpclient" % "4.3.3",
  "commons-io" % "commons-io" % "2.3"
)

playScalaSettings
