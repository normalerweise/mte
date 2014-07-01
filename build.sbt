//import sbtdocker.{Dockerfile, Plugin}
//import Plugin._
//import Plugin.DockerKeys._
import sbt._
import Keys._


name := """mte"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

// enable improved (experimental) incremental compilation algorithm called "name hashing"
incOptions := incOptions.value.withNameHashing(true)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies ++= Seq(
  // crosscutting concerns: logging, config, runtime...
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.7", // redirect the apache http lib logging to slf4j
  "org.slf4j" % "log4j-over-slf4j" % "1.7.7", // redirect the apache http lib logging to slf4j
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.kenshoo" %% "metrics-play" % "0.1.3",
  "nl.grons" %% "metrics-scala" % "3.2.0_a2.2",
  // WEB
  "org.webjars" %% "webjars-play" % "2.2.1-2",
  "org.webjars" % "angularjs" % "1.2.14",
  "org.webjars" % "angular-ui-router" % "0.2.8-2",
  "org.webjars" % "angular-ui-bootstrap" % "0.10.0-1",
  "org.webjars" % "angular-moment" % "0.6.2",
  "org.webjars" % "momentjs" % "2.5.1-1",
  // nlp
  "org.dbpedia.extraction" % "core" % "4.0-SNAPSHOT"
    exclude("log4j", "log4j"),
  "de.unihd.dbs" % "heideltime-standalone" % "1.7",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1",
  "de.uni-mannheim.dws" % "WikiParser" % "0.0.1-SNAPSHOT"
    exclude("commons-logging","commons-logging")
    exclude("edu.stanford.nlp", "stanford-corenlp")
    exclude("log4j", "log4j"),
  "ch.weisenburger" %% "mtner" % "1.0-SNAPSHOT"
    exclude("edu.stanford.nlp", "stanford-corenlp"),
  // SPARQL
  "org.apache.jena" % "jena-arq" % "2.11.2"
    exclude("log4j", "log4j")
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("commons-logging", "commons-logging"),
  // DATA
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
  //  exclude("org.apache.logging.log4j", "log4j-to-slf4j")
  //  exclude("org.apache.logging.log4j", "log4j-core")
  //  exclude("org.apache.logging.log4j", "log4j-api")
)

play.Project.playScalaSettings

//
//dockerSettings
//
//
//// Make docker depend on the package task, which generates a jar file of the application code
//docker <<= docker.dependsOn(`dist` in(Compile, packageBin))
//
//// Tell docker at which path the jar file will be created
//jarFile in docker <<= (artifactPath in(Compile, packageBin)).toTask
//
//// Create a custom Dockerfile
//dockerfile in docker <<= (stageDir in docker, jarFile in docker, mainClass in Compile) map {
//  (stageDir, jarFile, mainClass) => new Dockerfile {
//    from("totokaka/arch-java")
//    // Install scala
//    run("pacman", "-S", "--noconfirm", "scala")
//    // Add the generated jar file
//    add(jarFile, file(jarFile.getName))(stageDir)
//    // Run the jar file with scala library on the class path
//    entryPoint("java", "-cp", s"/usr/share/scala/lib/scala-library.jar:${jarFile.getName}", mainClass.get)
//  }
//}
//
//// Set a custom image name
//imageName in docker <<= (organization, name, version) map
//  ((organization, name, version) => s"$organization/$name:v$version")

