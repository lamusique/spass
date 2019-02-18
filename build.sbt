// Spaß
name := """spass"""

version := "0.1.0"

organization := "nekopiano"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

//crossScalaVersions := Seq("2.11.12", "2.12.7")

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test
libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided"

libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.7.0"


// logging
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "5.3"
//libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.21"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.lihaoyi" %% "sourcecode" % "0.1.5"

