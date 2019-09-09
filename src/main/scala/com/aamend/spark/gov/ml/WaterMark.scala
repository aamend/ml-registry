package com.aamend.spark.gov.ml

import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param._
import org.apache.spark.ml.util._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset}

class WaterMark(override val uid: String) extends Transformer with WatermarkParams {

  def setWatermarkColumn(value: String): this.type = set(watermarkColumnParam, value)
  def setWatermark(value: String): this.type = set(watermarkParam, value)

  setDefault(
    watermarkColumnParam -> watermarkColumnParamDefault,
    watermarkParam -> watermarkParamDefault
  )

  def this() = this(Identifiable.randomUID("watermark"))

  override def transform(dataset: Dataset[_]): DataFrame = {
    dataset.withColumn($(watermarkColumnParam), lit($(watermarkParam)))
  }

  override def copy(extra: ParamMap): Transformer = {
    defaultCopy(extra)
  }

  override def transformSchema(schema: StructType): StructType = {
    val inputFields = schema.fields
    require(!inputFields.exists(_.name == $(watermarkColumnParam)), s"Output column ${$(watermarkColumnParam)} already exists.")
    schema.add(StructField($(watermarkColumnParam), StringType, nullable = false))
  }

}

object WaterMark extends DefaultParamsReadable[WaterMark] {
  override def load(path: String): WaterMark = super.load(path)
}