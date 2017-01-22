import Dependencies._

name := "sbt-golo"

organization := "fr.ramiro"

scalaVersion := "2.10.6"

version      := "0.1.0-SNAPSHOT"

sbtPlugin := true

libraryDependencies ++= Seq(
  scalaTest % Test,
  "com.jsuereth" %% "scala-arm" % "2.0"
)
