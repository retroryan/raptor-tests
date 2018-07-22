name := """reactive-fauna"""
organization := "raptor"

version := "1.1"

resolvers += Resolver.mavenLocal

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .enablePlugins(JavaAppPackaging)


scalaVersion := "2.12.4"

libraryDependencies += guice

libraryDependencies ++= Seq(
  "com.faunadb" % "faunadb-java" % "2.2.0",
  "com.iheart" %% "ficus" % "1.4.3",
  "com.spotify" % "futures-extra" % "3.1.3-SNAPSHOT"
)