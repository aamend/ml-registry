package com.aamend.spark.ml.maven

import java.io.File

import org.eclipse.aether.artifact
import org.eclipse.aether.artifact.DefaultArtifact

/**
 * Wrapper object to maintain maven metadata, version and artifacts
 * @param groupId referencing your model in your organisation, it starts with a reversed domain name you control such as [com.aamend]
 * @param artifactId identifies your pipeline uniquely across all projects in your organisation
 * @param version the version object maintaining major, minor and build number
 * @param extension the artifact extension (inferred when adding a file)
 * @param classifier the artifact classifier (inferred when adding a file)
 * @param file a file to attach to this artifact. Only pom.xml and jar are supported.
 */
case class Artifact(
                     groupId: String,
                     artifactId: String,
                     version: Version,
                     extension: Option[String] = None,
                     classifier: Option[String] = None,
                     file: Option[File] = None
                   ) {

  /**
   * Attaching a physical file to your created artifact. Both the classifier and extension will be derived from the file name.
   * Note that we only support pom.xml and jar file.
   * @param file the file to attach
   * @return the original artifact enriched with file
   */
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

  /**
   * Converts our artifact into a maven artifact using Aether library
   * @return a maven artifact that can be deployed to remote repositories
   */
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

  /**
   * Maven coordinates use the following values: groupId, artifactId, version (GAV).
   * Provided an artifact GAV as String, we extract the relevant artifact information.
   * Note that version should follow [majorRelease].(([minorRelease]).([buildNumber])) pattern.
   * @param gav the pipeline GAV as semi column value separated, [groupId]:[artifactId]:[version]
   * @return the created artifact
   */
  def apply(gav: String): Artifact = {
    val a = gav.split(":")
    a.length match {
      case 2 => Artifact(a(0), a(1), Version())
      case 3 => Artifact(a(0), a(1), Version(a(2)))
      case _ => throw new IllegalArgumentException("Invalid artifact name, should be [groupId]:[artifactId]:[version]")
    }
    val Array(groupId, artifactId, version) = gav.split(":", 3)
    Artifact(
      groupId,
      artifactId,
      Version(version)
    )
  }

}