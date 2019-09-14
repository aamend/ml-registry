package com.aamend.spark.ml

import org.apache.spark.ml.{Pipeline, PipelineStage}

class VersionedPipeline extends Pipeline with WatermarkParams {

  def setWatermarkColumn(value: String): this.type = set(watermarkColumnParam, value)
  setDefault(watermarkColumnParam -> watermarkColumnParamDefault)

  override def setStages(value: Array[_ <: PipelineStage]): VersionedPipeline.this.type = {
    // Attaching watermark object that will be enriched at model deployment phase
    val watermark = new Watermark().setWatermarkColumn($(watermarkColumnParam))
    super.setStages(value :+ watermark)
  }

}
