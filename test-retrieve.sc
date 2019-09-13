//spark-shell --conf spark.jars.ivySettings=ivysettings.xml --packages com.aamend.spark:hello-world:1.0.2

import org.apache.spark.sql.SparkSession
import com.aamend.spark.ml._

val spark = SparkSession.builder().getOrCreate()
val testing = spark.createDataFrame(Seq((4L, "spark"), (5L, "jenkins"), (6L, "nexus"), (7L, "hadoop"))).toDF("id", "text")
val model = loadPipelineModel("hello-world")
model.transform(testing).show()