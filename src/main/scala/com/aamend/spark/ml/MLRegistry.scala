package com.aamend.spark.ml

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.Files

import com.aamend.spark.ml.maven.{Artifact, NexusMLRegistry, Version}
import com.aamend.spark.ml.io._
import org.apache.spark.ml.PipelineModel

import scala.io.Source

trait MLRegistry {
  def getNextVersion(artifact: Artifact): Version
  def deploy(artifacts: List[Artifact]): Unit
}

object MLRegistry {

  def resolve(modelGav: String): PipelineModel = {

    // pipeline will be exported from classpath to local disk
    val tempDir = Files.createTempDirectory("spark-governance").toFile
    val tempPipFile = new File(tempDir, "pipeline")
    tempDir.deleteOnExit()

    // Extract pipeline to local disk
    val rootPath = getClassPathFolder(modelGav)
    extractPipelineFromClasspath(tempPipFile, rootPath)

    // Load pipeline from local disk
    PipelineModel.load(tempPipFile.toURI.toString)
  }

  def deploy(pipelineModel: PipelineModel, modelGav: String): String = {
    val repository = NexusMLRegistry()
    deploy(pipelineModel, modelGav, repository)
  }

  def deploy(pipelineModel: PipelineModel, modelGav: String, repoId: String, repoUrl: String, repoUsername: String, repoPassword: String): String = {
    val repository = NexusMLRegistry(repoId, repoUrl, repoUsername, repoPassword)
    deploy(pipelineModel, modelGav, repository)
  }

  private def deploy(pipelineModel: PipelineModel,
             modelGav: String,
             repository: MLRegistry
            ): String = {

    val artifact = Artifact(modelGav)

    // Retrieve latest available version on Nexus
    val version = getNextVersion(artifact, repository)
    val newArtifact = artifact.copy(version = version)

    // Enrich pipeline with version number - if versioned pipeline
    pipelineModel.stages.find(_.isInstanceOf[Watermark]).map(transformer => {
      transformer.
        asInstanceOf[Watermark].
        setWatermark(newArtifact.toString)
    })

    // Package artifacts
    val artifacts = prepare(pipelineModel, newArtifact)

    // Deploy pipeline model to nexus
    repository.deploy(artifacts)

    // Return updated version
    artifacts.head.toString

  }

  private def prepare(
                       pipelineModel: PipelineModel,
                       artifact: Artifact
                     ): List[Artifact] = {

    // Storing pipeline on local disk (temp folder)
    val tempDir = Files.createTempDirectory("spark-governance").toFile
    tempDir.deleteOnExit()

    // Extract pipeline parameters
    val xmlFile = new File(tempDir, Artifact.metadataFile)
    scala.xml.XML.save(xmlFile.toString, pipelineModel.extractMetadata.toXml)

    List(
      // Create POM file containing model specs
      artifact.addFile(preparePom(artifact, tempDir)),
      // Create JAR file containing model binaries
      artifact.addFile(preparePipelineModel(pipelineModel, artifact, tempDir)),
      // Create METADATA file containing model hyper parameters
      artifact.addFile(xmlFile)
    )
  }

  private def getNextVersion(
                              artifact: Artifact,
                              nexus: MLRegistry
                            ): Version = {

    artifact.version.buildNumber match {
      case Some(_) => artifact.version
      case None => nexus.getNextVersion(artifact)
    }
  }

  private def preparePom(artifact: Artifact, tempDir: File): File = {

    val tempPomFile = new File(tempDir, Artifact.pomFile)
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
    packagePipelineJar(tempPipFile, tempJarFile, getClassPathFolder(artifact.artifactId))
    tempJarFile
  }

}
