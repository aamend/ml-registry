package com.aamend.spark.ml.maven

import java.io.File
import java.nio.file.Files

import com.aamend.spark.ml.ModelRepository
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.maven.repository.internal._
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.deployment.DeployRequest
import org.eclipse.aether.impl._
import org.eclipse.aether.repository.{Authentication, LocalRepository, RemoteRepository}
import org.eclipse.aether.resolution._
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.repository.AuthenticationBuilder
import org.eclipse.aether.{DefaultRepositorySystemSession, RepositorySystem}

import scala.collection.JavaConversions._
import scala.util.{Success, Try}

object ModelRepositoryImpl {

  val config: Config = ConfigFactory.load()
  val repoId: Option[String] = Try(config.getString("model.repository.id")).toOption
  val repoUrl: Option[String] = Try(config.getString("model.repository.url")).toOption
  val repoUsername: Option[String] = Try(config.getString("model.repository.username")).toOption
  val repoPassword: Option[String] = Try(config.getString("model.repository.password")).toOption

  /**
   * Create a new Model repository instance where connection details are found from typesafe provided configuration.
   * An application.conf must be supplied and contains the following
   * - model.repository.id: the model repository Id
   * - model.repository.url: the model repository url
   * - model.repository.username: the model repository username
   * - model.repository.password: the plain text repository password
   * @return an instance of [[ModelRepositoryImpl]]
   */
  def apply(): ModelRepositoryImpl = {
    require(repoId.isDefined, "Model repository Id must be defined in [application.conf]")
    require(repoUrl.isDefined, "Model repository Url must be defined in [application.conf]")
    require(repoUsername.isDefined, "Model repository Username must be defined in [application.conf]")
    require(repoPassword.isDefined, "Model repository Password must be defined in [application.conf]")
    new ModelRepositoryImpl(repoId.get, repoUrl.get, repoUsername.get, repoPassword.get)
  }

  /**
   * Create a new Model repository instance where connection details are provided
   * @param repoId the model repository Id
   * @param repoUrl the model repository url
   * @param repoUsername the model repository username
   * @param repoPassword the plain text repository password
   * @return an instance of [[ModelRepositoryImpl]]
   */
  def apply(repoId: String, repoUrl: String, repoUsername: String, repoPassword: String): ModelRepositoryImpl = {
    new ModelRepositoryImpl(repoId, repoUrl, repoUsername, repoPassword)
  }

}

class ModelRepositoryImpl(repoId: String, repoUrl: String, repoUsername: String, repoPassword: String) extends ModelRepository {

  val localRepoTemp: File = Files.createTempDirectory("pipeline").toFile
  localRepoTemp.deleteOnExit()

  val locator = new DefaultServiceLocator
  locator.addService(classOf[ArtifactDescriptorReader], classOf[DefaultArtifactDescriptorReader])
  locator.addService(classOf[VersionResolver], classOf[DefaultVersionResolver])
  locator.addService(classOf[VersionRangeResolver], classOf[DefaultVersionRangeResolver])
  locator.addService(classOf[MetadataGeneratorFactory], classOf[SnapshotMetadataGeneratorFactory])
  locator.addService(classOf[MetadataGeneratorFactory], classOf[VersionsMetadataGeneratorFactory])
  locator.addService(classOf[RepositoryConnectorFactory], classOf[BasicRepositoryConnectorFactory])
  locator.addService(classOf[TransporterFactory], classOf[FileTransporterFactory])
  locator.addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])

  val repositorySystem: RepositorySystem = locator.getService(classOf[RepositorySystem])
  val repositoryLocal = new LocalRepository(localRepoTemp)

  val session: DefaultRepositorySystemSession = MavenRepositorySystemUtils.newSession
  session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, repositoryLocal))

  val authentication: Authentication = new AuthenticationBuilder().addUsername(repoUsername).addPassword(repoPassword).build()
  val repositoryReleases: RemoteRepository = new RemoteRepository.Builder(repoId, "default", repoUrl).setAuthentication(authentication).build()

  override def deploy(artifacts: List[Artifact]): Unit = {
    val deployRequest = new DeployRequest
    deployRequest.setRepository(repositoryReleases)
    artifacts.map(_.toAether).foreach(deployRequest.addArtifact)
    repositorySystem.deploy(session, deployRequest)
  }

  override def getNextVersion(artifact: Artifact): Version = {
    val versions = getVersion(artifact)
    versions match {
      case Nil => artifact.version.copy(buildNumber = Some(0))
      case _ => Version(versions.last).increment
    }
  }

  private def getVersion(artifact: Artifact): List[String] = {
    val min = artifact.version.copy(buildNumber = Some(0))
    val max = artifact.version.copy(minorVersion = artifact.version.minorVersion + 1, buildNumber = Some(0))
    getVersions(artifact, s"[$min,$max)")
  }

  /**
    * Retrieve all artifact versions from repository given the provided maven version pattern
    *
    * @see <a href="https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN402">https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN402</a>
    * @param artifact the artifact to retrieve, containing GroupId and ArtifactId as well as classifier
    * @param versionRange  the maven version specific pattern, could be exact match [1.0.0] or range [1,2.0)
    * @return the list of all matching versions available on repository, ordered by version number DESC
    * @throws VersionRangeResolutionException if any issue occurred querying repository for versions
    */
  private def getVersions(artifact: Artifact, versionRange: String): List[String] = {

    val request: VersionRangeRequest = new VersionRangeRequest

    val aether = new DefaultArtifact(
      artifact.groupId,
      artifact.artifactId,
      "pom",
      versionRange
    )

    request.setArtifact(aether)
    request.setRepositories(List(repositoryReleases))
    val result: VersionRangeResult = repositorySystem.resolveVersionRange(session, request)
    result
      .getVersions
      .toList
      .map(v => Try(Version.apply(v.toString)))
      .collect { case Success(str) => str }
      .sorted
      .map(_.toString)
  }

}
