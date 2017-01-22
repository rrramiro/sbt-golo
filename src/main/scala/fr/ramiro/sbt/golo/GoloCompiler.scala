package fr.ramiro.sbt.golo

import java.io.{File, FileInputStream, InputStream}
import sbt.Keys.TaskStreams
import sbt.classpath.ClasspathUtilities
import scala.language.reflectiveCalls
import resource._

object GoloCompiler {

  type GoloCompiler = {
    def compileTo(fileName: java.lang.String, in: InputStream, outputDir: File): Unit
  }

  type Problem = {
    def getDescription: String
  }

  type GoloCompilationException = Throwable {
    def getProblems: java.util.List[Problem]
  }

  def compile(classpath: Seq[File], sources: Seq[File], outputDir: File, streams: TaskStreams): Unit = {
    val compiler = ClasspathUtilities.toLoader(classpath).loadClass("org.eclipse.golo.compiler.GoloCompiler").newInstance().asInstanceOf[GoloCompiler]
    for (file <- sources) {
      managed(new FileInputStream(file)).foreach { in =>
        try {
          compiler.compileTo(file.getName, in, outputDir)
        } catch {
          case e: GoloCompilationException =>
            if (e.getMessage != null) streams.log.error(e.getMessage)
            if (e.getCause != null) streams.log.error(e.getCause.getMessage)
            import scala.collection.JavaConversions._
            for (problem <- e.getProblems) {
              streams.log.error(problem.getDescription)
            }
        }
      }
    }
  }
}