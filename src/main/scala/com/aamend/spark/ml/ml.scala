package com.aamend.spark

import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.param.{Param => MLParam}

import scala.xml.Elem

package object ml {

  final val watermarkColParamName = "watermarkCol"
  final val watermarkParamName = "watermark"

  case class MetadataParam(name: String, doc: String, value: String)
  case class MetadataStage(uid: String, clazz: String, params: Array[MetadataParam])
  case class Metadata(id: String, stages: Array[MetadataStage]) {
    def toXml: Elem = {
      <metadata>
        <pipeline id={id}>
          <stages>{stages.map(stage => {
            <stage id={stage.uid}>
              <class>{stage.clazz}</class>
              <params>{stage.params.map(param => {
                <param>
                  <name>{param.name}</name>
                  <value>{param.value}</value>
                  <description>{param.doc}</description>
                </param>})}
              </params>
            </stage>})}
          </stages>
        </pipeline>
      </metadata>
    }
  }

  implicit class PipelineModelImp(model: PipelineModel) {

    def deploy(modelGav: String): String = {
      MLRegistry.deploy(model, modelGav)
    }

    def deploy(modelGav: String, repoId: String, repoUrl: String, repoUsername: String, repoPassword: String): String = {
      MLRegistry.deploy(model, modelGav, repoId, repoUrl, repoUsername, repoPassword)
    }

    def extractMetadata: Metadata = {
      val stages = model.stages.map(stage => {

        val stageParams = stage.params.map(param => {
          val pDoc = param.doc
          val pName = param.name
          val pValue = stage.get[AnyRef](param.asInstanceOf[MLParam[AnyRef]]) match {
            case Some(x) => x.toString
            case None => stage.getDefault(param).map(_.toString).orNull
          }
          MetadataParam(pName, pDoc, pValue)
        }).filter(m => {
          m.value != null
        })
        MetadataStage(stage.uid, stage.getClass.getName, stageParams)
      })

      Metadata(model.uid, stages)
    }
  }
}
