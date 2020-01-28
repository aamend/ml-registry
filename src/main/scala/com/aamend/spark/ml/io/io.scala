package com.aamend.spark.ml

import java.io.{File, FileInputStream, FileOutputStream}
import java.net.URLDecoder
import java.util
import java.util.jar.{Attributes, JarEntry, JarFile, JarOutputStream, Manifest}

import org.apache.commons.io.IOUtils

import scala.annotation.tailrec

package object io {

  final val ROOT_PATH = "pipelines"

  def getClassPathFolder(artifactId: String): String = {
    ROOT_PATH + File.separator + artifactId
  }

  /**
   * Read content of a pipeline model stored on local FS and write a Jar file where model can be added to application classpath
   * @param inputPath pipeline model location on local FS
   * @param outputFile Jar file that will contain trained pipeline model
   * @param rootDir root classpath folder that will contain serialized pipeline model object (e.g. name of the model)
   */
  def packagePipelineJar(inputPath: File, outputFile: File, rootDir: String): Unit = {
    require(inputPath.exists() && inputPath.isDirectory, "Input path does not exist")
    require(!outputFile.exists(), "Output file already exists")
    require(outputFile.getName.endsWith("jar"), "Output file should have jar extension")
    val manifest = new Manifest()
    manifest.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    val jos = new JarOutputStream(new FileOutputStream(outputFile), manifest)
    inputPath.listFiles().foreach(file => {
      _createJarFile(jos, file, rootDir)
    })
    jos.close()
  }

  /**
   * Extracting a pipeline model from classpath, output model to local FS that can be read through Spark engine
   * @param outputPath Where pipeline model in classpath will be extracted to
   * @param rootDir The name of the pipeline we can find on classpath
   */
  def extractPipelineFromClasspath(outputPath: File, rootDir: String): Unit = {
    require(!outputPath.exists(), "Output directory already exists")
    List("metadata", "stages").flatMap(path => {
      val loader = Thread.currentThread().getContextClassLoader
      val url = loader.getResource(s"$rootDir/$path")
      val jarPath = url.getPath.substring(5, url.getPath.indexOf("!")) //strip out only the JAR file
      val jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))
      val entries = jar.entries
      listJarEntry(rootDir, entries).map(_.replaceFirst(s"$rootDir/", ""))
    }).foreach(resource => {
      val outputFile = new File(outputPath, resource)
      new File(outputFile.getParent).mkdirs()
      val os = new FileOutputStream(outputFile)
      IOUtils.copy(getClass.getResourceAsStream(s"/$rootDir/$resource"), os)
      os.close()
    })
  }

  private def _createJarFile(jos: JarOutputStream, file: File, rootDir: String): Unit = {
    val name = rootDir + File.separator + file.getName
    if (file.isFile) {
      val jarEntry = new JarEntry(name)
      jarEntry.setTime(file.lastModified())
      jos.putNextEntry(jarEntry)
      IOUtils.copy(new FileInputStream(file), jos)
      jos.closeEntry()
    } else if (file.isDirectory) {
      val dirName = name match {
        case x if !x.endsWith("/") => name + "/"
        case _ => name
      }
      val jarEntry = new JarEntry(dirName)
      jarEntry.setTime(file.lastModified())
      jos.putNextEntry(jarEntry)
      jos.closeEntry()
      file.listFiles().foreach(f => _createJarFile(jos, f, name))
    }
  }

  @tailrec
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
