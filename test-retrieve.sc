/*
spark-shell \
  --conf spark.jars.ivySettings=ivysettings.xml \
  --packages com.aamend.spark:hello-world:latest.release
*/

import org.apache.spark.sql.SparkSession
import com.aamend.spark.ml.ModelRepository

val spark = SparkSession.builder().getOrCreate()
val testing = spark.createDataFrame(Seq((4L, "spark"), (5L, "jenkins"), (6L, "nexus"), (7L, "hadoop"))).toDF("id", "text")
val model = ModelRepository.resolve("hello-world")
model.transform(testing).select("id", "text", "pipeline").show(10, truncate = false)
