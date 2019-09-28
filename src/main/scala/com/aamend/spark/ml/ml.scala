package com.aamend.spark

import org.apache.spark.ml.PipelineModel

package object ml {

  final val watermarkColumnParamName = "watermarkColumnParamName"
  final val watermarkColumnParamDefault = "pipeline"
  final val groupIdParamName = "groupId"
  final val artifactIdParamName = "artifactId"
  final val versionParamName = "version"
  final val defaultValue = "UNKNOWN"

  /**
   * Simple wrapper that contains pipeline GAV to enrich original dataframe post transformations.
   * @param groupId the pipeline model groupId to reflect model coordinates in model repository
   * @param artifactId the pipeline model artifactId to reflect model coordinates in model repository
   * @param version the pipeline model version to reflect model coordinates in model repository
   */
  case class PipelineWatermark(groupId: String, artifactId: String, version: String)

  implicit class PipelineDeploy(pipelineModel: PipelineModel) {
    /**
     * Deploy a trained pipeline model to model repository (nexus or artifactory)
     * @param gav the pipeline model coordinate to deploy
     * @return the exact GAV with correct version number and build number as deployed onto model repository
     */
    def deploy(gav: String): String = {
      ModelRepository.deploy(pipelineModel: PipelineModel, gav)
    }

    /**
     * Deploy a trained pipeline model to model repository (nexus or artifactory)
     * @param gav the pipeline model coordinate to deploy
     * @param repoId the nexus repository ID
     * @param repoUrl the nexus repository URL
     * @param repoUsername the nexus repository username
     * @param repoPassword the nexus repository password
     * @return the exact GAV with correct version number and build number as deployed onto model repository
     */
    def deploy(gav: String, repoId: String, repoUrl: String, repoUsername: String, repoPassword: String): String = {
      ModelRepository.deploy(pipelineModel: PipelineModel, gav, repoId, repoUrl, repoUsername, repoPassword)
    }
  }

  /**
   * Extract a model from classpath and load a spark pipeline model
   * @param pipelineId the root classpath folder containing serialized model. Usually the model artifactId
   * @return a trained pipeline model deserialized
   */
  def loadPipeline(pipelineId: String): PipelineModel = {
    ModelRepository.resolve(pipelineId)
  }

}
