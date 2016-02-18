enablePlugins(JavaServerAppPackaging)

name := "Reactive Buzz"

version := "0.1"

organization := "io.rob"

scalaVersion := "2.11.7"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  Resolver.bintrayRepo("hseeberger", "maven"))


addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")

val scalacheck = Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.2"
)

val scalatest = Seq(
  "org.scalatest" %% "scalatest" % "2.2.4"
)

val Json4sVersion     = "3.3.0"
val json4s = Seq(
  "org.json4s"          %% "json4s-native"   % Json4sVersion,
  "org.json4s"          %% "json4s-ext"      % Json4sVersion,
  "org.json4s"          %% "json4s-jackson"  % Json4sVersion,
  "de.heikoseeberger"   %% "akka-http-json4s" % "1.4.2"
)

val AkkaVersion       = "2.4.2"
val AkkaHttpVersion   = "2.0.3"
val akka = Seq(
  "com.typesafe.akka"   %% "akka-slf4j"      % AkkaVersion,
  "com.typesafe.akka"   %% "akka-http-experimental" % AkkaHttpVersion
)

val akkaTestKit = Seq(
  "com.typesafe.akka" %% "akka-testkit" % AkkaVersion
)


val logging = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "ch.qos.logback"      %  "logback-classic" % "1.1.2"
)

libraryDependencies ++= akka ++ json4s ++ akkaTestKit ++ scalacheck ++ scalatest ++ logging

// Assembly settings
mainClass in Global := Some("io.rob.Main")

jarName in assembly := "reactiveBuzz.jar"

