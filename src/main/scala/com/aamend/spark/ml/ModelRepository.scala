package com.aamend.spark.ml

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.Files

import com.aamend.spark.ml.maven.{Artifact, ModelRepositoryImpl, Version}
import com.aamend.spark.ml.io._
import org.apache.spark.ml.PipelineModel

import scala.io.Source

trait ModelRepository {
  def getNextVersion(artifact: Artifact): Version
  def deploy(artifacts: List[Artifact]): Unit
}

object ModelRepository {

  def save(pipelineModel: PipelineModel, path: String): Unit = {
    // Enrich pipeline with location
    pipelineModel.stages.find(_.isInstanceOf[Watermark]).map(transformer => {
      transformer.
        asInstanceOf[Watermark].
        setWatermark(path)
    })
    pipelineModel.save(path)
  }

  def resolve(modelId: String): PipelineModel = {
    val tempDir = Files.createTempDirectory("spark-governance").toFile
    tempDir.deleteOnExit()
    val tempPipFile = new File(tempDir, "pipeline")
    extractPipelineFromClasspath(tempPipFile, modelId)
    PipelineModel.load(tempPipFile.toURI.toString)
  }

  def deploy(pipelineModel: PipelineModel, modelGav: String): String = {
    val repository = ModelRepositoryImpl()
    deploy(pipelineModel, modelGav, repository)
  }

  def deploy(pipelineModel: PipelineModel, modelGav: String, repoId: String, repoUrl: String, repoUsername: String, repoPassword: String): String = {
    val repository = ModelRepositoryImpl(repoId, repoUrl, repoUsername, repoPassword)
    deploy(pipelineModel, modelGav, repository)
  }

  private def deploy(pipelineModel: PipelineModel,
             modelGav: String,
             repository: ModelRepository
            ) = {

    val artifact = Artifact(modelGav)
    val version = getNextVersion(artifact, repository)

    // Enrich pipeline with version number
    pipelineModel.stages.find(_.isInstanceOf[Watermark]).map(transformer => {
      transformer.
        asInstanceOf[Watermark].
        setWatermark(artifact.toString)
    })

    val artifacts = prepare(pipelineModel, artifact.copy(version = version))
    repository.deploy(artifacts)
    artifacts.head.toString
  }

  private def prepare(
                       pipelineModel: PipelineModel,
                       artifact: Artifact
                     ): List[Artifact] = {

    val tempDir = Files.createTempDirectory("spark-governance").toFile
    tempDir.deleteOnExit()
    val pom = artifact.addFile(preparePom(artifact, tempDir))
    val jar = artifact.addFile(preparePipelineModel(pipelineModel, artifact, tempDir))
    List(pom, jar)
  }

  private def getNextVersion(
                              artifact: Artifact,
                              nexus: ModelRepository
                            ): Version = {

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
