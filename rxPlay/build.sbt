name := "RxPlay-Example"

version := "1.0"

scalaVersion := "2.10.3"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq (
    "com.netflix.rxjava" % "rxjava-scala" % "0.14.5",
    "play" %% "play-iteratees" % "2.1.5"
)

