package com.aamend.spark.gov

import java.io.{File, FileInputStream, FileOutputStream}
import java.net.URLDecoder
import java.util
import java.util.jar._

import org.apache.commons.io.IOUtils

package object io {

  def packagePipelineJar(inputPath: File, outputFile: File, modelId: String): Unit = {
    require(inputPath.exists() && inputPath.isDirectory, "Input path does not exist")
    require(!outputFile.exists(), "Output file already exists")
    require(outputFile.getName.endsWith("jar"), "Output file should have jar extension")
    val manifest = new Manifest()
    manifest.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    val jos = new JarOutputStream(new FileOutputStream(outputFile), manifest)
    inputPath.listFiles().foreach(file => {
      _createJarFile(jos, file, modelId)
    })
    jos.close()
  }

  private def _createJarFile(jos: JarOutputStream, file: File, rootDir: String): Unit = {
    val name = rootDir + "/" + file.getName
    val jarEntry = new JarEntry(name)
    if (file.isFile && file.getName.startsWith("part-")) {
      jarEntry.setTime(file.lastModified())
      jos.putNextEntry(jarEntry)
      IOUtils.copy(new FileInputStream(file), jos)
      jos.closeEntry()
    } else if (file.isDirectory) {
      val dirName = name match {
        case x if !x.endsWith("/") => name + "/"
        case _ => name
      }
      jarEntry.setTime(file.lastModified())
      jos.putNextEntry(jarEntry)
      jos.closeEntry()
      file.listFiles().foreach(f => _createJarFile(jos, f, name))
    }
  }

  def extractPipelineFromClasspath(outputPath: File, modelId: String): Unit = {
    require(!outputPath.exists(), "Output directory already exists")
    List("metadata", "stages").flatMap(path => {
      val loader = Thread.currentThread().getContextClassLoader
      val url = loader.getResource(s"$modelId/$path")
      val jarPath = url.getPath.substring(5, url.getPath.indexOf("!")) //strip out only the JAR file
      val jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))
      val entries = jar.entries
      listJarEntry(modelId, entries).map(_.replaceFirst(s"$modelId/", ""))
    }).foreach(resource => {
      val outputFile = new File(outputPath, resource)
      new File(outputFile.getParent).mkdirs()
      val os = new FileOutputStream(outputFile)
      IOUtils.copy(getClass.getResourceAsStream(s"/$modelId/$resource"), os)
      os.close()
    })
  }

  @scala.annotation.tailrec
  private def listJarEntry(classPathRoot: String, entries: util.Enumeration[JarEntry], output: List[String] = List.empty[String]): List[String] = {
    if(entries.hasMoreElements) {
      val name = entries.nextElement.getName
      if (name.startsWith(classPathRoot) && !name.endsWith("/"))
        listJarEntry(classPathRoot, entries, output :+ name)
      else
        listJarEntry(classPathRoot, entries, output)
    } else {
      output
    }
  }

}
