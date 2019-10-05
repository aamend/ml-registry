/*
spark-shell \
  --conf spark.jars.ivySettings=ivysettings.xml \
  --files application.conf \
  --driver-java-options -Dconfig.file=application.conf \
  --packages com.aamend.spark:spark-governance:latest.release
*/

import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.sql.SparkSession
import com.aamend.spark.ml.VersionedPipeline
import com.aamend.spark.ml.ModelRepository
import org.apache.spark.ml.PipelineModel

val spark = SparkSession.builder().getOrCreate()
val training = spark.createDataFrame(Seq((0L, "spark", 1.0), (1L, "jenkins", 0.0), (2L, "nexus", 1.0), (3L, "hadoop", 0.0))).toDF("id", "text", "label")
val tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
val hashingTF = new HashingTF().setNumFeatures(1000).setInputCol(tokenizer.getOutputCol).setOutputCol("features")
val lr = new LogisticRegression().setMaxIter(10).setRegParam(0.001)
val pipeline = new VersionedPipeline().setWatermarkCol("pipeline").setStages(Array(tokenizer, hashingTF, lr))
val model = pipeline.fit(training)
ModelRepository.deploy(model, "com.aamend.spark:hello-world:1.0")
ModelRepository.save(model, "/tmp/hello-world")
PipelineModel.load("/tmp/hello-world").transform(training).select("id", "text", "pipeline").show(10, truncate = false)