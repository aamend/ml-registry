## Spark ML Governance package

This project aims at bringing a key building block that makes
Spark ML actionable in corporate environment - *model devops and governance*.

## Principles

We enrich Spark ML framework to enable governance of machine learning models,
leveraging software delivery tools such as [apache maven](https://maven.apache.org/), [Ivy](http://ant.apache.org/ivy/) and [nexus](https://www.sonatype.com/product-nexus-repository). 

- Use `maven` to version a trained pipeline model and package binary as `.jar` file
- Use `nexus` as a central model registry to deploy immutable ML binaries
- Use `ivy` to load specific models to Spark context via `--packages` functionality

With a central repository for ML models, machine learning can be constantly retrained and re-injected
into your operation environment as reported in below HL workflow. 

Note that it is common for companies to have different environments between exploration, testing and operation. 
It is therefore critical to have a central repository for ML models.

![ml-flow](ml-flow.png)

One can operate data science under full governance where each
models can be trusted, validated, registered, deployed and continuously improved, 
useful when applied to lambda architecture and / or online machine learning.

The key concepts are explained below
- [Maven Versioning](###maven-versioning)
- [Model Registry](###model-registry)

Alternatively, jump to usage section
- [Pipeline Deployment](###deploy-pipeline)
- [Pipeline Resolution](###resolve-pipeline)
- [Pipeline Versioning](###versioned-pipeline)

###Maven Versioning

We propose a naming convention for machine learning models borrowed from standard 
software delivery principles (maven), in the form of

```
[groupId]:[artifactId]:[majorVersion].[minorVersion].[buildNumber]
```

- `groupId`: The name of your organisation, using reversed domain (e.g. `com.organisation`)
- `artifactId`: The name of your pipeline, agnostic from the modelling technique used (e.g. `customer-propensity`)
- `majorVersion`: The major version of your model. Specific to the technique and features used
- `minorVersion`: The minor version of your model. A specific configuration was used but technique remained the same (version increment should be backwards compatible)
- `buildNumber`: The build number that will be incremented automatically any time a same model is retrained using same configuration and same technique but with up to date data

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

###Model Registry

We use Nexus as a central model registry (could be artifactory).
Setting up Nexus is relatively easy and should be a de facto standard in your organisation already. 

A requirement is to have a maven2 release repository created to host versioned pipeline models. 
Models will be deployed and maintained as per any standard Java dependency.

![ml-registry](model_repository.png)

Note that we purposely did not enable `SNAPSHOT` feature of machine learning models as we consider each iteration 
of a model as an immutable release, hence with version build number increment.
 
To operate under full governance, it is advised to use multiple repositories (e.g. staging and prod) where only validated
models (e.g. validated through a QA process) can be promoted from one to another via Nexus built-in functionality (out of scope). 

## Usage

Using this project in your spark environment

```shell script
spark-shell --packages com.aamend.spark:spark-governance:latest.release
```

###Deploy Pipeline

Inspired by the scikit-learn project, spark ML relies on Pipeline to execute machine learning workflows at scale.
Spark stores binaries in a "crude" way, simply serializing models metadata to a given path (hdfs or s3).

```scala
val pipeline: Pipeline = new Pipeline().setStages(stages)
val model: PipelineModel = pipeline.fit(df)
model.save("/path/to/hdfs")
```

We propose the following changes

```scala
import com.aamend.spark.ml._
ModelRepository.deploy(model, "com.aamend.spark:hello-world:1.0")
```

This process will
 
- Serialize model to disk as per standard ML pipeline `save` function
- Package pipeline model as a `.jar` file 
- Works out the relevant build number for artifact `com.aamend.spark:hello-world` given a major and minor version
- Upload model `com.aamend.spark:hello-world:latest` to nexus

Nexus authentication is enabled by passing an `application.conf` to your spark context as follows

```shell script
spark-shell \
  --files application.conf \
  --driver-java-options -Dconfig.file=application.conf \
  --packages com.aamend.spark:spark-governance:latest.release
```

or adding `application.conf` in your project classpath. 
Configuration is as follows

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

Alternatively, we can pass nexus credentials to our `deploy` function explicitly

```scala
import com.aamend.spark.ml._
ModelRepository.deploy(
  model = model,
  gav = "com.aamend.spark:hello-world:1.0",
  repoId = "model-governance",
  repoURL = "http://localhost:8081/nexus/content/repositories/model-governance/",
  repoUsername = "admin",
  repoPassword = "admin123"
)
```

###Resolve Pipeline

Given that we consider each pipeline model as a standard maven dependency available on nexus, 
we can leverage Spark Ivy functionality (through `--packages`) to inject our model as a dependency to a spark context. 
Note that one needs to pass specific ivy settings to point to their internal Nexus repository. 
An example of `ivysettings.xml` can be found [here](ivysettings.xml)

```shell script
spark-shell \
  --conf spark.jars.ivySettings=ivysettings.xml \
  --packages com.aamend.spark:hello-world:latest.release
```

By specifying `latest.release` instead of specific version, Ivy framework will ensure latest version of our 
model is resolved and loaded, paving the way to online machine learning. 
Under the hood, we read pipeline metadata from classpath, 
store binaries to disk and load pipeline model through native spark `load` function.

```scala
import com.aamend.spark.ml._
val model: PipelineModel = ModelRepository.resolve("hello-world")
model.transform(df)
```

###Versioned Pipeline

In order to guarantee model reproducibility, we enabled a new type of pipeline `VersionedPipeline` that appends a 
schema with pipeline version as published to nexus.

```scala
import com.aamend.spark.ml._
val pipeline: Pipeline = new VersionedPipeline().setWatermarkCol("pipeline").setStages(stages)
val model: PipelineModel = pipeline.fit(df)
ModelRepository.deploy(model, "com.aamend.spark:hello-world:1.0")
```

For each record, we know the exact version of the model used, model available on nexus. 

```scala
import com.aamend.spark.ml._
val model: PipelineModel = ModelRepository.resolve("hello-world")
model.transform(df).select("id", "pipeline").show()
```

```
+---+----------------------------------+
|id |pipeline                          |
+---+----------------------------------+
|4  |com.aamend.spark:hello-world:1.0.0|
|5  |com.aamend.spark:hello-world:1.0.0|
|6  |com.aamend.spark:hello-world:1.0.0|
|7  |com.aamend.spark:hello-world:1.0.0|
+---+----------------------------------+
```

Ideally, this extra information at a record level will serve for model monitoring. 
As a data scientist, you may want to be informed whenever performance of your model degrade so that you can
retrain a new model and deploy via the above methodology.

## Backlog

- serialize models using MLeap to be used outside of a spark context (e.g. Flink)
- extract data distribution from model input features to be stored on nexus as metadata

## Install

```shell script
mvn clean package
```

### Author

[Antoine Amend](mailto:antoine.amend@gmail.com)
