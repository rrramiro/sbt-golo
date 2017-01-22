package fr.ramiro.sbt.golo

import java.io.File

import sbt._
import Keys._

object GoloPlugin extends AutoPlugin {

  object autoImport {
    lazy val goloVersion = settingKey[String]("Golo version")
    lazy val goloSource = settingKey[File]("Default golo source directory")
    lazy val goloc = taskKey[Seq[File]]("Compile Golo sources")
  }
  import autoImport._

  lazy val golocFilter : ScopeFilter = ScopeFilter(inDependencies(ThisProject, transitive = true, includeRoot = false), inConfigurations(Compile), inTasks(goloc))

  lazy val compileFilter : ScopeFilter = ScopeFilter(inDependencies(ThisProject, transitive = true, includeRoot = false), inConfigurations(Compile))

  override val projectSettings = Seq(
    goloVersion := "3.2.0-M5",
    libraryDependencies ++= Seq(
      "org.eclipse.golo" % "golo" % goloVersion.value,
      "com.beust" % "jcommander" % "1.60"
    ),
    managedClasspath in goloc := {
      val ct = (classpathTypes in goloc).value
      val report = update.value
      Classpaths.managedJars(Compile, ct, report)
    },
    goloSource in Compile := (sourceDirectory in Compile).value / "golo",
    unmanagedSourceDirectories in Compile += {(goloSource in Compile).value},
    classDirectory in (Compile, goloc) := (classDirectory in Compile).value,
    managedClasspath in goloc <<= (classpathTypes in goloc, update) map { (ct, report) =>
      Classpaths.managedJars(Compile, ct, report)
    },
    goloc in Compile := {
      val sourceDirectory = (goloSource in Compile).value
      val sources = (sourceDirectory ** "*.golo").get
      if(sources.nonEmpty){
        val s: TaskStreams = streams.value
        s.log.info(s"Start Compiling Golo sources : ${sourceDirectory.getAbsolutePath} ")
        val classpath = (managedClasspath in goloc).value.files ++
          classDirectory.all(compileFilter).value ++
          classDirectory.all(golocFilter).value ++
          Seq((classDirectory in Compile).value) ++
          (managedClasspath in Compile).value.files

        val destinationDirectory = (classDirectory in (Compile, goloc)).value

        GoloCompiler.compile(classpath, sources, destinationDirectory, s)

        ((destinationDirectory ** "*.class").get pair relativeTo(destinationDirectory)).map{ case(k,v) =>
          IO.copyFile(k, (resourceManaged in Compile).value / v, preserveLastModified = true)
          (resourceManaged in Compile).value / v
        }
      }
      else{
        Seq.empty
      }
    },
    resourceGenerators in Compile <+= goloc in Compile,
    fullClasspath in Runtime += Attributed.blank((classDirectory in (Compile, goloc)).value),
    (compile in Compile) := ((compile in Compile) dependsOn (goloc in Compile)).value
  )
}
