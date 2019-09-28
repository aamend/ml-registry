## Spark ML Governance package

Doing actual data science is more than just training machine learning models;
it is a delicate balance between advanced statistics, complex engineering,
modern devops technologies, and model governance.

We tend to see governance as something we have to comply with,
something related to audit and regulation, something less experienced data
scientists see as a *blocker to innovation*.
We could not be farther from the truth; model governance is what makes an
organisation successful with their AI strategy.
When operating data science under some sort of governance framework, we ensure each
models can be trusted, validated, registered, deployed and continuously improved.

Spark ML is one of the best framework to deliver advanced analytics
as it bridges the gap between science and engineering natively.
This project aims at bringing a key building block that makes
data science actionable in corporate environment - *model devops and governance*.

## Principles

We enrich Spark ML framework to enable governance of machine learning models,
leveraging software delivery tools such as maven and nexus. In short, this library helps you

- Use `maven` to version trained pipeline models as `.jar` files
- Use `nexus` as model registry to deploy immutable binaries
- Load specific models natively using `--packages` functionality

Following such a discipline, one can reach the holy grail of data science,
**online machine learning**, where new models are constantly retrained and "injected"
to your operation environment (could be in real time in a typical lambda architecture) as reported in below HL workflow.

![ml-flow](ml-flow.png)

### Maven versioning

We propose a naming convention for machine learning models borrowed from standard software delivery principles (maven), in the form of

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

The corresponding maven GAV coordinates of your ML pipeline would be as follows

```xml
<dependency>
    <groupId>com.organisation</groupId>
    <artifactId>customer-propensity</artifactId>
    <version>2.0.0</version>
</dependency>
```

The goal of that project is to be able to automatically ingest the above dependency to your operation environment 
for model scoring via a **model registry**

### Model registry

We use Nexus as model registry. Setting up Nexus is relatively easy and should already be a de facto standard in your organisation. 
The requirement is to have a maven2 repository created to host machine learning models.

Models will be deployed as per standard Java dependencies

![ml-registry](model_repository.png)

Note that we purposely did not enable `SNAPSHOT` feature of machine learning models as we consider each iteration 
of a model as immutable release (any new model will result in version increment). 
To operate under full governance, it is advise to use multiple repositories (e.g. prod and test) where only validated
models (e.g. through QA) can be promoted from one Nexus repository to another. 
Out of scope here, this could be enabled through standard devops methodologies.

## Usage

```shell script
spark-shell --packages com.aamend.spark:spark-governance:1.0
```

### Deploy pipeline

Mostly inspired by the scikit-learn project, spark ML relies on Pipeline to execute machine learning workflows at scale.
Spark stores binaries in a crude way by serializing pipeline models directly to a given path.

```scala
val df: DataFrame = ???
val stages: Array[Transformer] = ???
val pipeline = new Pipeline().setStages(stages)
val model = pipeline.fit(df)
model.save("/path/to/hdfs")
```

Given that we will be using a model registry and a `.jar` packaging (with specific version), 
we propose the following changes

```scala
import com.aamend.spark.ml._
model.deploy(gav = "com.aamend.spark:hello-world:1.0")
```

This process will
 
- package our pipeline model as a `.jar` file by first serializing to disk as per standard ML pipeline 
feature (`save` function)
- works out the relevant version number for artifact `com.aamend.spark:hello-world`. 
If model registry already has a version `1.0.0`, this new build will be `1.0.1`
- create the relevant maven dependencies for model binaries (add this project as model dependency)
- upload model `com.aamend.spark:hello-world` to nexus

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

or passing credentials to `deploy` function explicitly

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

Given that we treat trained pipeline model as standard maven dependency on nexus, 
we can leverage standard `--packages` functionality to inject our model to a spark context as follows.

```shell script
spark-shell \
  --conf spark.jars.ivySettings=ivysettings.xml \
  --packages com.aamend.spark:hello-world:latest.release
```

In this case, we want to load latest release of model `com.aamend.spark:hello-world`.

```scala
val df: DataFrame = ???
import com.aamend.spark.ml._
val model = loadPipelineModel("hello-world")
model.transform(df)
```

Under the hood, we copy the classpath folder "hello-world" (the model artifact ID) 
back to disk and leverage `load` function of standard ML pipeline.

### Versioned pipeline

To guarantee full model governance, we enabled a new type of pipeline `VersionedPipeline` that append your schema with 
the additional structure.

```
root
 |-- ...
 |-- pipeline: struct (nullable = true)
 |    |-- groupId: string (nullable = true)
 |    |-- artifactId: string (nullable = true)
 |    |-- version: string (nullable = true)
```

At training phase, we attach a custom transformer to your pipeline that we update when deploying model to nexus.

```scala
val df: DataFrame = ???
val stages: Array[Transformer] = ???

import com.aamend.spark.ml._

// Appending transformer to existing pipeline
val pipeline = new VersionedPipeline()
  .setWatermarkColumn("pipeline")
  .setStages(stages)

// Enriching pipeline with model GAV at deploy time
model.deploy("com.aamend.spark:hello-world:1.0")
```

For each scored record, we know the exact version (and build number) of model used, model that can be found on Nexus. 
Given the immutability of model registry, we enabled full model reproducibility.

## Backlog

- serialize models using MLeap
- use monitoring system to extract model metrics (i.e. data distribution used)

## Install

```shell script
mvn clean package
```

### Author

Antoine Amend
