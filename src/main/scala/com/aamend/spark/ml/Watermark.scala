package com.aamend.spark.ml

import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param._
import org.apache.spark.ml.util._
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset}

class Watermark(override val uid: String) extends Transformer with WatermarkParams {

  def setWatermarkColumn(value: String): this.type = set(watermarkColumnParam, value)
  def setGroupId(value: String): this.type = set(groupIdParam, value)
  def setArtifactId(value: String): this.type = set(artifactIdParam, value)
  def setVersion(value: String): this.type = set(versionParam, value)

  setDefault(
    watermarkColumnParam -> watermarkColumnParamDefault,
    groupIdParam -> defaultValue,
    artifactIdParam -> defaultValue,
    versionParam -> defaultValue
  )

  def this() = this(Identifiable.randomUID("watermark"))

  val addWatermark: UserDefinedFunction = udf(() => {
    PipelineWatermark(
      $(groupIdParam),
      $(artifactIdParam),
      $(versionParam)
    )
  })

  override def transform(dataset: Dataset[_]): DataFrame = {
    dataset.withColumn($(watermarkColumnParam), addWatermark())
  }

  override def copy(extra: ParamMap): Transformer = {
    defaultCopy(extra)
  }

  override def transformSchema(schema: StructType): StructType = {

    val inputFields = schema.fields
    require(!inputFields.exists(_.name == $(watermarkColumnParam)), s"Output column ${$(watermarkColumnParam)} already exists.")

    val append = StructField($(watermarkColumnParam), StructType(
      Array(
        StructField(groupIdParamName, StringType, nullable = false),
        StructField(artifactIdParamName, StringType, nullable = false),
        StructField(versionParamName, StringType, nullable = false)
      )
    ), nullable = false)

    schema.add(append)
  }

}

object Watermark extends DefaultParamsReadable[Watermark] {
  override def load(path: String): Watermark = super.load(path)
}