package com.aamend.spark

import org.apache.spark.ml.PipelineModel

package object ml {

  final val watermarkColumnParamName = "watermarkColumnParamName"
  final val watermarkColumnParamDefault = "pipeline"
  final val groupIdParamName = "groupId"
  final val artifactIdParamName = "artifactId"
  final val versionParamName = "version"
  final val defaultValue = "UNKNOWN"

  case class PipelineWatermark(groupId: String, artifactId: String, version: String)

  implicit class PipelineDeploy(pipelineModel: PipelineModel) {
    def deploy(modelGav: String): String = {
      ModelRepository.deploy(pipelineModel: PipelineModel, modelGav)
    }
  }

  def loadPipelineModel(pipelineId: String): PipelineModel = {
    ModelRepository.resolve(pipelineId)
  }

  def loadPipeline(modelId: String): PipelineModel = {
    ModelRepository.resolve(modelId)
  }
}
