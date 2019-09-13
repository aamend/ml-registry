package com.aamend.spark.ml

import java.io.File

import org.eclipse.aether.artifact
import org.eclipse.aether.artifact.DefaultArtifact

case class Artifact(
                     groupId: String,
                     artifactId: String,
                     version: Version,
                     extension: Option[String] = None,
                     classifier: Option[String] = None,
                     file: Option[File] = None
                   ) {

  def addFile(file: File): Artifact = {
    require(file.exists && file.isFile)
    this.copy(
      file = Some(file),
      classifier = Some(""),
      extension = Some(getExtension(file))
    )
  }

  override def toString: String = {
    s"$groupId:$artifactId:${version.toString}"
  }

  def toAether: artifact.Artifact = {

    require(file.isDefined)
    require(extension.isDefined)
    require(classifier.isDefined)

    new DefaultArtifact(
      groupId,
      artifactId,
      classifier.get,
      extension.get,
      version.toString
    ).setFile(file.get)

  }

  private def getExtension(file: File): String = {
    file.getName match {
      case "pom.xml" => "pom"
      case x if x.endsWith(".jar") => "jar"
      case _ => throw new IllegalArgumentException(s"File must be [jar] or [pom.xml]")
    }
  }

}

object Artifact {

  def apply(coord: String): Artifact = {
    val a = coord.split(":")
    a.length match {
      case 2 => Artifact(a(0), a(1), Version())
      case 3 => Artifact(a(0), a(1), Version(a(2)))
      case _ => throw new IllegalArgumentException("Invalid artifact name, should be [groupId]:[artifactId]:[version]")
    }
    val Array(groupId, artifactId, version) = coord.split(":", 3)
    Artifact(
      groupId,
      artifactId,
      Version(version)
    )
  }

}