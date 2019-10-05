package com.aamend.spark.ml

import org.apache.spark.ml.{Pipeline, PipelineStage}

class VersionedPipeline extends Pipeline with WatermarkParams {

  def setWatermarkCol(value: String): this.type = set(watermarkColParam, value)
  setDefault(watermarkColParam -> "pipeline")

  override def setStages(value: Array[_ <: PipelineStage]): VersionedPipeline.this.type = {
    val watermark = new Watermark().setWatermarkCol($(watermarkColParam))
    super.setStages(value :+ watermark)
  }

}
