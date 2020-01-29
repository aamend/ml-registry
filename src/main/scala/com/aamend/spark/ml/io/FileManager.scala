package com.aamend.spark.ml.io

import java.io.File
import java.nio.file.Files

object FileManager {

  private var pipelineDir: Option[File] = None

  def getPath: File = {
    if(pipelineDir.isDefined) {
      pipelineDir.get
    } else {
      pipelineDir = Some(Files.createTempDirectory("pipelines").toFile)
      pipelineDir.get.deleteOnExit()
      pipelineDir.get
    }
  }

}
