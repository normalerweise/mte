import sbtdocker.{Dockerfile, Plugin}
import Plugin._
import Plugin.DockerKeys._
import sbt._
import Keys._


name := """mte"""

version := "1.0-SNAPSHOT"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.1-2",
  "org.webjars" % "angularjs" % "1.2.14",
  "org.webjars" % "angular-ui-router" % "0.2.8-2",
  "org.webjars" % "angular-ui-bootstrap" % "0.10.0-1",
  "org.webjars" % "angular-moment" % "0.6.2",
  "org.webjars" % "momentjs" % "2.5.1-1",
  "org.dbpedia.extraction" % "core" % "4.0-SNAPSHOT",
  "de.unihd.dbs" % "heideltime-standalone" % "1.5",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1",
  "org.apache.jena" % "jena-arq" % "2.11.1" excludeAll(ExclusionRule(organization = "org.slf4j", name="slf4j-log4j12")),
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
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

