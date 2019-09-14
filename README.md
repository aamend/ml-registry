## Spark ML Governance package


## Train and deploy a new pipeline

```shell script
spark-shell \
  --files application.conf \
  --driver-java-options -Dconfig.file=application.conf \
  --packages com.aamend.spark:spark-governance:latest.snapshots
```

```scala
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.sql.SparkSession
import com.aamend.spark.ml._

val spark = SparkSession.builder().getOrCreate()
val training = spark.createDataFrame(Seq((0L, "spark", 1.0), (1L, "jenkins", 0.0), (2L, "nexus", 1.0), (3L, "hadoop", 0.0))).toDF("id", "text", "label")
val tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
val hashingTF = new HashingTF().setNumFeatures(1000).setInputCol(tokenizer.getOutputCol).setOutputCol("features")
val lr = new LogisticRegression().setMaxIter(10).setRegParam(0.001)
val pipeline = new VersionedPipeline().setWatermarkColumn("pipeline").setStages(Array(tokenizer, hashingTF, lr))
val model = pipeline.fit(training)
model.deploy("com.aamend.spark:hello-world:1.0")
```

## Access and execute model

```shell script
spark-shell \
  --conf spark.jars.ivySettings=ivysettings.xml \
  --packages com.aamend.spark:hello-world:latest.release
```

```scala
import org.apache.spark.sql.SparkSession
import com.aamend.spark.ml._

val spark = SparkSession.builder().getOrCreate()
val testing = spark.createDataFrame(Seq((4L, "spark"), (5L, "jenkins"), (6L, "nexus"), (7L, "hadoop"))).toDF("id", "text")
val model = loadPipelineModel("hello-world")
model.transform(testing).select("id", "text", "pipeline").show(10, truncate = false)
```

### Install

```shell script
mvn clean package
```

### Author

Antoine Amend