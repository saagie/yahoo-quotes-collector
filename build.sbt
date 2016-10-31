name := "yahoo-quotes-collector"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.2"
libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "2.4.2"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.4.2"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.9.0.1"

mainClass in Compile := Some("io.saagie.devoxx.ma.QuotesCollector")
