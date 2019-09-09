package com.aamend.spark.gov.ml

import java.io.{File, FileWriter}
import java.nio.file.Files

import com.aamend.spark.gov.io.{extractPipelineFromClasspath, packagePipelineJar}
import com.aamend.spark.gov.nexus.{Artifact, Nexus}
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import org.apache.maven.model.{Model => M2Model}
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.param.Param
import org.apache.spark.sql.DataFrame

case class AuditablePipelineModel (parent: PipelineModel) {

  private def prepare(nexus: Nexus, artifact: Artifact): List[Artifact] = {

    val tempDir = Files.createTempDirectory("spark-governance").toFile
    tempDir.deleteOnExit()

    val tempPipFile = new File(tempDir, "pipeline")
    val tempJarFile = new File(tempDir, "pipeline.jar")
    val tempPomFile = new File(tempDir, "pom.xml")

    // Get next version available on nexus
    val version = if(artifact.version.buildNumber.isDefined) {
      artifact.version
    } else {
      nexus.getNextVersion(artifact)
    }

    // Update artifact version to next version available
    val artifactToSave = artifact.copy(version = version)

    // Watermark pipeline
    val watermark = artifactToSave.toString
    watermarkPipeline(watermark)

    // Save pipeline model locally and build jar
    parent.save(tempPipFile.toURI.toString)
    packagePipelineJar(tempPipFile, tempJarFile, artifactToSave.artifactId)

    // Build pom.xml
    val m2Model = new M2Model()
    m2Model.setGroupId(artifactToSave.groupId)
    m2Model.setArtifactId(artifactToSave.artifactId)
    m2Model.setVersion(artifactToSave.version.toString)
    new MavenXpp3Writer().write(new FileWriter(tempPomFile), m2Model)

    // Return list of artifacts to deploy
    List(
      artifactToSave.addFile(tempPomFile),
      artifactToSave.addFile(tempJarFile)
    )
  }

  def save(path: String): Unit = {
    watermarkPipeline(path)
    parent.save(path)
  }

  def deploy(artifactString: String): String = {
    val artifact = Artifact(artifactString)
    deploy(artifact)
  }

  private def deploy(artifact: Artifact): String = {
    val nexus = new Nexus()
    val artifacts = prepare(nexus, artifact)
    nexus.deploy(artifacts)
    artifacts.head.toString
  }

  def transform(df: DataFrame): DataFrame = parent.transform(df)

  private def watermarkPipeline(watermark: String): Unit = {
    val watermarkOpt = parent.stages.find(_.isInstanceOf[WaterMark])
    require(watermarkOpt.isDefined, "Watermark must be have been added to Auditable pipeline")
    val stage = watermarkOpt.get
    stage.set[String](stage.params.find(_.name == watermarkParamName).get.asInstanceOf[Param[String]], watermark)
  }

}

object AuditablePipelineModel {

  def resolve(modelId: String): AuditablePipelineModel = {
    val tempDir = Files.createTempDirectory("spark-governance").toFile
    val tempPipFile = new File(tempDir, "pipeline")
    extractPipelineFromClasspath(tempPipFile, modelId)
    val model = PipelineModel.load(tempPipFile.toURI.toString)
    tempDir.delete()
    AuditablePipelineModel(model)
  }

}
