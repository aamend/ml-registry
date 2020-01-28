package com.aamend.spark.ml

import java.io.File
import java.nio.file.Files

import com.aamend.spark.ml.io._
import org.scalatest.{FlatSpec, Matchers}

class PackagingTest extends FlatSpec with Matchers {

  "A pipeline stored on disk" should "be packaged as JAR" in {

    // Loading test pipeline from test folder
    val resourceName = "pipeline"
    val classLoader = getClass.getClassLoader
    val pipelinePath = new File(classLoader.getResource(resourceName).getFile).getAbsolutePath

    // Packaging test pipeline as JAR file
    val jarDir = Files.createTempDirectory("pipelines")
    try {
      val jarFile = new File(jarDir.toFile, "pipeline.jar")
      val rootDir = getClassPathFolder("unit-test")
      packagePipelineJar(new File(pipelinePath), jarFile, rootDir)
      assert(jarFile.getTotalSpace > 0)
    } catch {
      case _: Exception => assert(false)
    } finally {
      jarDir.toFile.delete()
    }
  }

}
