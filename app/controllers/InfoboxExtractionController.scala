package controllers

import play.api.libs.concurrent.Akka
import play.api.mvc.{Action, Controller}
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import java.io.File
import play.api.libs.json.{Writes, Json}
import play.api.libs.Files
import ch.weisenburger.dbpedia.extraction.mappings.RuntimeAnalyzer
import scala.io.Codec
import models.{ExtractionRun, Revision, Quad}
import actors.{ExtractInfoboxAndSaveQuads, DefaultActors}
import play.api.Logger


/**
 * Created by Norman on 20.03.14.
 */
object InfoboxExtractionController extends Controller {
  implicit val codec = Codec.UTF8

  def listExtractedData = Action.async {
    val extractedFileNames = scala.concurrent.Future {
      val folder = new File("")

      val files = folder.listFiles.toList
      files.map(_.getName)
    }
    extractedFileNames.map(fileNames => Ok(Json.toJson(fileNames)))
  }

  //  def extractSample = Action {
  //    val sample = Helper.readRandomSample
  //    sample.take(2).foreach( page =>
  //    Akka.system.scheduler.scheduleOnce(1000.microsecond) {
  //      extractWikiPage(page)
  //    })
  //
  //    Ok("ok")
  //  }

  implicit val runtimeWrites = new Writes[RuntimeAnalyzer] {
    def writes(r: RuntimeAnalyzer) = Json.obj(
      "page" -> r.page,
      "total" -> r.getTotalRuntime,
      "posTagger" -> r.getPosTaggeRuntime._1,
      "heidelTime" -> r.getHeidelTimeRuntime._1,
      "timexFormatter" -> r.getTimexFormaterRuntime._1,
      "wikiParser" -> r.getWikiParserRuntime._1,
      "jcasCreation" -> r.getJcasRuntime._1,
      "dbPediaExtractor" -> r.getExtractorRuntime._1,
      "posTaggerRel" -> r.getPosTaggeRuntime._2.toString,
      "heidelTimeRel" -> r.getHeidelTimeRuntime._2.toString,
      "timexFormatterRel" -> r.getTimexFormaterRuntime._2.toString,
      "wikiParserRel" -> r.getWikiParserRuntime._2.toString,
      "jcasCreationRel" -> r.getJcasRuntime._2.toString,
      "dbPediaExtractorRel" -> r.getExtractorRuntime._2.toString)
  }


  def extractExtractionRunResources(extractionRunId: String) = Action {
    import DefaultActors._
    Akka.system.scheduler.scheduleOnce(100.microsecond) {
      val extractionRunData = ExtractionRun.getById(extractionRunId)

      extractionRunData.onSuccess {
        case Some(run) =>
          val resourcesSize = run.getResources.size
          run.getResources.view.zipWithIndex.foreach {
            case (resource, index) =>
              infoboxExtractor ! ExtractInfoboxAndSaveQuads(run.id, run.description, index+1, resourcesSize,resource._1) }
      }
      extractionRunData.onFailure {
        case ex => throw ex
      }
    }

    Ok
  }


  implicit val quadWrites = new Writes[Quad] {
    def writes(quad: Quad) = Json.obj(
      "subject" -> quad.subject,
      "predicate" -> quad.predicate,
      "object" -> quad.obj,
      "context" -> quad.context)
  }

}
