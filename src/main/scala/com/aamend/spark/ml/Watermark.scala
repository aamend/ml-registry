package com.aamend.spark.ml

import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param._
import org.apache.spark.ml.util._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset}

class Watermark(override val uid: String) extends Transformer with WatermarkParams {

  def setWatermarkCol(value: String): this.type = set(watermarkColParam, value)
  def setWatermark(value: String): this.type = set(watermarkParam, value)

  setDefault(
    watermarkColParam -> "watermark",
    watermarkParam -> "UNKNOWN"
  )

  def this() = this(Identifiable.randomUID("watermark"))

  override def transform(dataset: Dataset[_]): DataFrame = {
    dataset.withColumn($(watermarkColParam), lit($(watermarkParam)))
  }

  override def copy(extra: ParamMap): Transformer = {
    defaultCopy(extra)
  }

  override def transformSchema(schema: StructType): StructType = {
    val inputFields = schema.fields
    require(!inputFields.exists(_.name == $(watermarkColParam)), s"Output column ${$(watermarkColParam)} already exists.")
    val append = StructField($(watermarkColParam), StringType, nullable = false)
    schema.add(append)
  }

}

object Watermark extends DefaultParamsReadable[Watermark] {
  override def load(path: String): Watermark = super.load(path)
}