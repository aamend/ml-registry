package com.aamend.spark

import org.apache.spark.ml.PipelineModel

package object ml {

  final val watermarkColumnParamName = "watermarkColumnParamName"
  final val watermarkColumnParamDefault = "watermark"
  final val watermarkParamName = "watermarkParamName"
  final val watermarkParamDefault = "UNKNOWN"

  implicit class ModelDeploy(pipelineModel: PipelineModel) {

    def deploy(modelGav: String): String = {
      AuditPipelineModel.deploy(pipelineModel: PipelineModel, modelGav)
    }
  }

  def loadPipelineModel(modelId: String): PipelineModel = {
    AuditPipelineModel.resolve(modelId)
  }
}
