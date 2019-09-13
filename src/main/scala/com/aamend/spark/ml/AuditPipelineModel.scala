package com.aamend.spark.ml

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.Files

import MLUtils.{extractPipelineFromClasspath, packagePipelineJar}
import org.apache.spark.ml.PipelineModel

import scala.io.Source

object AuditPipelineModel {

  //TODO: configure
  val nexusUrl = "http://localhost:8081/nexus/content/repositories/releases"
  val nexusUsername = "admin"
  val nexusPassword = "admin123"

  def resolve(modelId: String): PipelineModel = {
    val tempDir = Files.createTempDirectory("spark-governance").toFile
    val tempPipFile = new File(tempDir, "pipeline")
    extractPipelineFromClasspath(tempPipFile, modelId)
    val model = PipelineModel.load(tempPipFile.toURI.toString)
    tempDir.delete()
    model
  }

  def deploy(pipelineModel: PipelineModel, modelGav: String): String = {
    val nexus = new Nexus(nexusUrl, nexusUsername, nexusPassword)
    val artifact = Artifact(modelGav)
    val version = getNextVersion(artifact, nexus)
    val artifacts = prepare(pipelineModel, artifact.copy(version = version))
    nexus.deploy(artifacts)
    artifacts.head.toString
  }

  private def prepare(pipelineModel: PipelineModel, artifact: Artifact): List[Artifact] = {
    val tempDir = Files.createTempDirectory("spark-governance").toFile
    val pom = artifact.addFile(preparePom(artifact, tempDir))
    val jar = artifact.addFile(preparePipelineModel(pipelineModel, artifact, tempDir))
    List(pom, jar)
  }

  private def getNextVersion(artifact: Artifact, nexus: Nexus): Version = {
    artifact.version.buildNumber match {
      case Some(_) => artifact.version
      case None => nexus.getNextVersion(artifact)
    }
  }

  private def preparePom(artifact: Artifact, tempDir: File): File = {

    val tempPomFile = new File(tempDir, "pom.xml")
    val pomStr = Source.
      fromInputStream(this.getClass.getResourceAsStream("/pom.xml")).
      getLines().
      mkString("\n").
      format(artifact.groupId, artifact.artifactId, artifact.version.toString)

    val bw = new BufferedWriter(new FileWriter(tempPomFile))
    bw.write(pomStr)
    bw.close()
    tempPomFile
  }

  private def preparePipelineModel(pipelineModel: PipelineModel, artifact: Artifact, tempDir: File): File = {
    val tempPipFile = new File(tempDir, "pipeline-model")
    val tempJarFile = new File(tempDir, "pipeline-model.jar")
    pipelineModel.save(tempPipFile.toURI.toString)
    packagePipelineJar(tempPipFile, tempJarFile, artifact.artifactId)
    tempJarFile
  }

}
