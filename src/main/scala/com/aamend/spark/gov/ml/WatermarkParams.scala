package com.aamend.spark.gov.ml

import org.apache.spark.ml.param.{Param, Params}
import org.apache.spark.ml.util.DefaultParamsWritable

trait WatermarkParams extends Params with DefaultParamsWritable {
  final val watermarkColumnParam = new Param[String](this, watermarkColumnParamName, s"The output column containing model version or location (default $watermarkColumnParamDefault)")
  final val watermarkParam = new Param[String](this, watermarkParamName, s"The model version or location (default $watermarkParamDefault)")
}
