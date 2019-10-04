## Spark ML Governance package

Spark ML is one of the best framework to deliver advanced analytics
as it bridges the gap between science and engineering natively.
This project aims at bringing a key building block that makes
data science actionable in corporate environment - *model devops and ML governance*.

## Principles

We enrich Spark ML framework to enable governance of machine learning models,
leveraging software delivery tools such as [apache maven](https://maven.apache.org/), [Ivy](http://ant.apache.org/ivy/) and [nexus](https://www.sonatype.com/product-nexus-repository). 
In short, this library helps you

- Use `maven` to version a trained pipeline model and package binary as `.jar` file
- Use `nexus` as a central model registry to deploy immutable ML binaries
- Load specific models to Spark context via `--packages` and its Ivy functionality

With a central repository for ML models, machine learning can be constantly retrained and "injected"
to your operation environment (e.g. in a lambda architecture) as reported in below HL workflow. 
Note that it is common for companies to have different environments between exploration, testing and operation. 
It is therefore critical to have a central repository for ML models.

![ml-flow](ml-flow.png)

One can operate data science under full governance where each
models can be trusted, validated, registered, deployed and continuously improved.

### Maven versioning

We propose a naming convention for machine learning models borrowed from standard 
software delivery principles (maven), in the form of

```
[groupId]:[artifactId]:[majorVersion].[minorVersion].[buildNumber]
```

- `groupId`: The name of your organisation, using reversed domain (e.g. `com.organisation`)
- `artifactId`: The name of your pipeline, agnostic from the modelling technique used (e.g. `customer-propensity`)
- `majorVersion`: The major version of your model. Specific to the technique and features used, a version increment may not be backwards compatible
- `minorVersion`: The minor version of your model. A specific configuration was used but technique remained the same. Version increment should be backwards compatible
- `buildNumber`: The build number that will be incremented automatically any time a same model is retrained using same configuration and same technique but with new data

An example of valid naming convention would be

    `com.organisation:customer-propensity:1.0.0`
    `com.organisation:customer-propensity:1.0.1`
    `com.organisation:customer-propensity:1.1.0`
    `com.organisation:customer-propensity:2.0.0`

The corresponding maven GAV (**G**roup **A**rtifact **V**ersion) will be as follows

```xml
<dependency>
    <groupId>com.organisation</groupId>
    <artifactId>customer-propensity</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Model registry

We use Nexus as a central model registry, though principle remain the same using Artifactory or any other maven repository.
Setting up Nexus is relatively easy and should already be a de facto standard in your organisation. 

Requirement is to have a maven2 repository created to host our machine learning models. 
Models will be deployed and maintained as per any standard Java dependency.

![ml-registry](model_repository.png)

Note that we did not enable `SNAPSHOT` feature of machine learning models as we consider each iteration 
of a model as an immutable release (any new model will result in version build number increment).
 
To operate under full governance, it is advised to use multiple repositories (e.g. staging and prod) where only validated
models (e.g. validated through a QA process) can be promoted from one to another via Nexus functionality (out of scope here). 

## Usage

Using this project in your spark environment

```shell script
spark-shell --packages com.aamend.spark:spark-governance:1.0
```

### Deploy pipeline

Inspired by the scikit-learn project, spark ML relies on Pipeline to execute machine learning workflows at scale.
Spark stores binaries in a "crude" way by serializing models metadata directly to a given path.

```scala
val df: DataFrame = ???
val stages: Array[Transformer] = ???
val pipeline: Pipeline = new Pipeline().setStages(stages)
val model: PipelineModel = pipeline.fit(df)
model.save("/path/to/hdfs")
```

We propose the following changes

```scala
import com.aamend.spark.ml._
model.deploy("com.aamend.spark:hello-world:1.0")
```

This process will
 
- Serialize model to disk as per standard ML pipeline `save` function
- Package our pipeline model as a `.jar` file 
- Works out the relevant version number for artifact `com.aamend.spark:hello-world` given major and minor version
- Upload model `com.aamend.spark:hello-world:latest` to nexus

Nexus authentication is enabled by passing an `application.conf` to your spark context as follows

```shell script
model {
    repository {
        id: "model-governance"
        url: "http://localhost:8081/nexus/content/repositories/model-governance/"
        username: "admin"
        password: "admin123"
    }
}
```

or passing credentials to our `deploy` function explicitly

```scala
import com.aamend.spark.ml._
model.deploy(
  gav = "com.aamend.spark:hello-world:1.0",
  repoId = "model-governance",
  repoURL = "http://localhost:8081/nexus/content/repositories/model-governance/",
  repoUsername = "admin",
  repoPassword = "admin123"
)
```

### Retrieve pipeline

Given that we consider each pipeline model as a standard maven dependency available on nexus, 
we can leverage Spark awesome `--packages` functionality to inject our model to a spark context as follows. 
Note that you need to pass specific ivy settings to point Ivy resolver to our internal Nexus repository. 
An example of `ivysettings.xml` can be found [here](ivysettings.xml)

```shell script
spark-shell \
  --conf spark.jars.ivySettings=ivysettings.xml \
  --packages com.aamend.spark:hello-world:1.0.0
```

By specifying `latest.release` instead of specific version, Ivy framework will ensure latest version of our 
model is resolved and loaded, paving the way to online machine learning.

```scala
val df: DataFrame = ???

import com.aamend.spark.ml._
val model: PipelineModel = loadPipelineModel("hello-world")
model.transform(df)
```

### Versioned pipeline

To guarantee model reproducibility, we enabled a new type of pipeline `VersionedPipeline` that appends your schema with 
the additional structure reflecting deployed artifact.

```
root
 |-- ...
 |-- pipeline: struct (nullable = true)
 |    |-- groupId: string (nullable = true)
 |    |-- artifactId: string (nullable = true)
 |    |-- version: string (nullable = true)
```

```scala
val df: DataFrame = ???
val stages: Array[Transformer] = ???

import com.aamend.spark.ml._
val pipeline: Pipeline = new VersionedPipeline()
  .setWatermarkColumn("pipeline")
  .setStages(stages)

val model: PipelineModel = pipeline.fit(df)
model.deploy("com.aamend.spark:hello-world:1.0")
```

For each scored record, we know the exact version (and build number) of the model used (available on nexus). 

```scala
import com.aamend.spark.ml._
val model: PipelineModel = loadPipelineModel("hello-world")
model.transform(df).select("id", "pipeline").show()
```

```
+---+--------------------------------------+
|id |pipeline                              |
+---+--------------------------------------+
|4  |[com.aamend.spark, hello-world, 1.0.0]|
|5  |[com.aamend.spark, hello-world, 1.0.0]|
|6  |[com.aamend.spark, hello-world, 1.0.0]|
|7  |[com.aamend.spark, hello-world, 1.0.0]|
+---+--------------------------------------+
```

Ideally, this extra information at the record level will serve for model monitoring. 
As a data scientist, you want to be informed whenever performance of your model degrade so that you can
retrain a new model and deploy via the above methodology.

## Backlog

- serialize models using MLeap to use outside of a spark context (e.g. Flink)
- use monitoring system to extract model metrics (i.e. data distribution used)

## Install

```shell script
mvn clean package
```

### Author

Antoine Amend
