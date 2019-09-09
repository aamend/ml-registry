package com.aamend.spark.gov.ml

import org.apache.spark.ml.param.Param
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.sql.Dataset

class AuditablePipeline extends Pipeline with Serializable {

  def setWatermarkColumn(value: String): this.type = set(watermark, value)
  val watermark: Param[String] = new Param(this, watermarkColumnParamName, s"The output column containing model coordinates (default $watermarkColumnParamDefault)")

  setDefault(
    watermark -> watermarkColumnParamDefault
  )

  override def setStages(value: Array[_ <: PipelineStage]): this.type = {
    val waterMark = new WaterMark().setWatermarkColumn($(watermark))
    set(stages, value.asInstanceOf[Array[PipelineStage]] :+ waterMark)
    this
  }

  def train(dataset: Dataset[_]): AuditablePipelineModel = {
    AuditablePipelineModel(super.fit(dataset))
  }

}