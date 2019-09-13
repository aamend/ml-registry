package com.aamend.spark.ml

import java.io.File

import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FunSuite}

trait SparkSpec extends FunSuite with BeforeAndAfterAll {

  def sparkTest(name: String)(f: SparkSession => Unit): Unit = {

    this.test(name) {

      val spark = SparkSession
        .builder()
        .appName(name)
        .master("local")
        .config("spark.default.parallelism", "1")
        .getOrCreate()

      try {
        f(spark)
      } finally {
        spark.stop()
      }
    }
  }

  override def afterAll() {
    val warehouse = new File("spark-warehouse")
    if(warehouse.exists())
      warehouse.delete()

    val derby = new File("derby.log")
    if(derby.exists())
      derby.delete()
  }

}
