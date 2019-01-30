//import AssemblyKeys._  // put this at the top of the file

scalaVersion := "2.12"

name := "bibliodata"

organization := "bibliodata"

version := "1.0-SNAPSHOT"

publishMavenStyle := true
crossPaths := false
autoScalaLibrary := false

mainClass in (Compile, run) := Some("bibliodata.core.CitationNetworkRetriever")

libraryDependencies ++= Seq(
   "org.jsoup" % "jsoup" % "1.11.3",
   "org.apache.httpcomponents" % "httpcore" % "4.4.10",
   "org.apache.httpcomponents" % "httpclient" % "4.5.6",
   "org.apache.commons" % "commons-lang3" % "3.8.1"

)

//assemblySettings
//packSettings
