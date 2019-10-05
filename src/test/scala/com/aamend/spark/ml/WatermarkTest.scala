package com.aamend.spark.ml

import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest.Matchers

class WatermarkTest extends SparkSpec with Matchers {

  implicit class SparkImpl(spark: SparkSession) {
    def getDF: DataFrame = {
      import spark.implicits._
      """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit,
        |sed do eiusmod tempor incididunt
        |ut labore et dolore magna aliqua.
        |Ut enim ad minim veniam, quis nostrud
        |exercitation ullamco laboris nisi ut
        |aliquip ex ea commodo consequat.
        |Duis aute irure dolor in reprehenderit
        |in voluptate velit esse cillum dolore
        |eu fugiat nulla pariatur.
        |Excepteur sint occaecat cupidatat non
        |proident, sunt in culpa qui officia
        |deserunt mollit anim id est laborum.
        |""".stripMargin.split("\\n").toSeq.toDF("text")
    }
  }

  sparkTest("testing watermarked pipeline") { spark =>

    val training = spark.getDF

    // Configure an ML pipeline with non existing column
    val waterMark = new Watermark().setWatermarkCol("pipeline").setWatermark("GROUP:ARTIFACT:VERSION")
    val pipeline = new Pipeline().setStages(Array(waterMark))

    // Make sure all versions are consistent
    val df = pipeline.fit(training).transform(training).cache()
    df.show(100, truncate = false)
    df.select("pipeline").rdd.map(r => r.getAs[String]("pipeline")).collect().toSet should be(Set("GROUP:ARTIFACT:VERSION"))
  }

  sparkTest("testing pipeline with existing watermarked field") { spark =>

    val training = spark.getDF.withColumn("version", lit(1))

    // Configure an ML pipeline with existing column
    val waterMark = new Watermark().setWatermarkCol("version")
    val pipeline = new Pipeline().setStages(Array(waterMark))

    // An exception should be raised 'outputCol already exist'
    intercept[IllegalArgumentException] {
      pipeline.fit(training)
    }

  }

}