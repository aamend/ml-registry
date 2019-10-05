package com.aamend.spark.ml

import org.apache.spark.ml.param.{Param, Params}
import org.apache.spark.ml.util.DefaultParamsWritable

trait WatermarkParams extends Params with DefaultParamsWritable {
  final val watermarkColParam = new Param[String](this, watermarkColParamName, s"The output column containing pipeline version")
  final val watermarkParam = new Param[String](this, watermarkParamName, s"The pipeline version")
}
