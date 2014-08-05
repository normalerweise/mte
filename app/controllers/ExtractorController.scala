package controllers

import play.api.libs.concurrent.Akka
import play.api.mvc.{Action, Controller}
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import java.io.File
import extractors.{Quad, SinglePageTemporalExtractor, RelevantRevisionDownloader}
import play.api.libs.json.{Writes, Json}
import scalax.io.support.FileUtils
import play.api.libs.Files
import ch.weisenburger.dbpedia.extraction.mappings.RuntimeAnalyzer
import scala.io.Codec


/**
 * Created by Norman on 20.03.14.
 */
object ExtractorController extends Controller {
  implicit val codec = Codec.UTF8

  def listExtractedData = Action.async {
    val extractedFileNames = scala.concurrent.Future {
      val folder = new File(savePath)

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

  def extractSample = Action {

    Akka.system.scheduler.scheduleOnce(100.microsecond) {
        val sample = Helper.readRandomSample
        sample.foreach( page =>
        extractWikiPage(page))

        //RuntimeAnalyzer.all.foreach(t => println(t.aggPosTaggerlTime))

        val json = Json.toJson(RuntimeAnalyzer.all)
        val prettyJsonString = Json.prettyPrint(json)
        Files.writeFile(Helper.ensureExists("data/runtime.json"), prettyJsonString)
        RuntimeAnalyzer.clear
      }

    Ok("ok")
  }


  import play.api.libs.functional.syntax._

  implicit val quadWrites = new Writes[Quad] {
    def writes(quad: Quad) = Json.obj(
      "subject" -> quad.subject,
      "predicate" -> quad.predicate,
      "object" -> quad.obj,
      "context" -> quad.context)
  }

  val savePath = "data/triples_over_time/"
  private def extractWikiPage(resourceUri: String) = {
    val pageName = resourceUri.split('/').last
    println("started to extract " + pageName)
    val result = SinglePageTemporalExtractor.extractPage(pageName)
    val resultJson = Json.toJson(result)
    val prettyJsonString = Json.prettyPrint(resultJson)
    Files.writeFile(Helper.ensureExists(savePath + pageName + ".json"), prettyJsonString)
    println("finished " + pageName)
  }

}
