package com.aamend.spark.ml

import org.apache.spark.ml.param.{Param, Params}
import org.apache.spark.ml.util.DefaultParamsWritable

trait WatermarkParams extends Params with DefaultParamsWritable {
  final val watermarkColumnParam = new Param[String](this, watermarkColumnParamName, s"The output column containing model GAV")
  final val groupIdParam = new Param[String](this, groupIdParamName, s"The model groupId")
  final val artifactIdParam = new Param[String](this, artifactIdParamName, s"The model artifactId")
  final val versionParam = new Param[String](this, versionParamName, s"The model version")
}
