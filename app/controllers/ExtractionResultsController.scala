/**
 * Created by Norman on 20.03.14.
 */
package controllers

import play.api.mvc.{Action, Controller}
import scala.io.Codec
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models._

import play.api.libs.concurrent.Akka
import actors.events.EventLogger
import models.EventTypes._
import models.Quad
import play.api.Play.current
import extraction.formatters.{TurtleSaver, QuadsMerger}


object ExtractionResultsController extends Controller {

  import play.api.libs.json._


  implicit val codec = Codec.UTF8

  case class PageStats(pageName: String, properties: Seq[(String, Int, List[String])])

  implicit val quadReads: Reads[Quad] = ExtractionRunPageResultJsonConverter.quadMongoReads

  implicit val pageStatsWrites = new Writes[PageStats] {
    def writes(pageStats: PageStats) = {
      val props = pageStats.properties.map(prop => prop._1 -> Json.toJson(Json.obj("numberOfYears" -> prop._2, "years" -> prop._3)))
      val name = Seq(("pageName" -> Json.toJson(pageStats.pageName)))
      val res = name ++ props
      Json.toJson(res.toMap)
    }
  }

  def getExtractionRunEventStats(extractionRunId: String) = Action.async {
    Event.eventStatsForRun(extractionRunId).map(res => Ok(Json.prettyPrint(Json.toJson(res))))
  }


  def getExtractionRunStats(extractionRunId: String) = Action.async {
    val run = ExtractionRunPageResult.get(extractionRunId)

    val statsPerPage = run.map(_.par.map(quadToStats(_)))

    val aggregatesPerProperty = statsPerPage.map(stats =>
      stats.flatMap(_.properties).groupBy(_._1).toSeq.map { stat =>
        val propertyName = stat._1
        val size = stat._2.size.toDouble
        val _3years = stat._2.filter(_._2 >= 3)
        val _5years = _3years.filter(_._2 >= 5)

        val _3yearsRel = round(_3years.size / size)
        val _5yearsRel = round(_5years.size / size)

        (propertyName, Json.obj("_3years" -> _3years.size, "_3yearsRel" -> _3yearsRel, "_5years" -> _5years.size, "_5yearsRel" -> _5yearsRel))
      })

    val result = for {
      perPage <- statsPerPage
      aggregates <- aggregatesPerProperty
    } yield Json.obj(
        "noOfPages" -> perPage.size,
        "aggregated" -> Json.toJson(aggregates.toMap.seq),
        "perPage" -> Json.toJson(perPage.toList))

    result.map(res => Ok(Json.prettyPrint(res)))
  }

  def listExtractionRunResults(extractionRunId: String) = Action.async {
    ExtractionRunPageResult.listAsJson(extractionRunId).map(results => Ok(Json.toJson(results)))
  }

  def getExtractionRunResults(extractionRunId: String) = Action.async {
    ExtractionRunPageResult.getAsJson(extractionRunId).map(results => Ok(Json.toJson(results)))
  }

  def getExtractionRunResultsOfPage(extractionRunId: String, pageTitle: String) = Action.async {
    ExtractionRunPageResult.getAsJson(extractionRunId, pageTitle).map(results => Ok(Json.prettyPrint(Json.toJson(results))))
  }

  def getAll = Action.async {
    //future { Ok("test")}
    ExtractionRunPageResult.getAllAsJson.map(results => Ok(Json.toJson(results)))
  }

  import scala.concurrent.duration._

  def quadsToRDF(extractionRunId: String) = Action {
    Akka.system.scheduler.scheduleOnce(100 microseconds) {
      ExtractionRunPageResult.get(extractionRunId).map {
        pageExtractionResults =>
          // speedup by using parallel map
          val quadsToSave = pageExtractionResults.par.map { page =>
            // TODO: For now year precision is sufficient
            val quadsWithYearPrecision = page.quads.map(convertToYearPrecision)
            QuadsMerger.getDistinctQuads(quadsWithYearPrecision)
          }
          TurtleSaver.save(s"data/$extractionRunId.tt", quadsToSave.flatten.seq)
          EventLogger raise Event(convertedResultsToRDF, s"Converted Results to RDF for Run: ${extractionRunId}")
      }
    }
    Ok
  }

  val convertToYearPrecision = (q: Quad) => {
    val from = q.context.get("fromDate") match {
      case Some(year) => Seq("fromDate" -> year.substring(0, 4))
      case None => Seq.empty
    }
    val to = q.context.get("toDate") match {
      case Some(year) => Seq("toDate" -> year.substring(0, 4))
      case None => Seq.empty
    }
    val newQ = q.copy(context = q.context ++ from ++ to)
    newQ
  }

  def getResultsAsRDF(extractionRunId: String) = Application.dataFile(extractionRunId + ".tt")


  private def quadToStats(extractionResult: ExtractionRunPageResult) = {
    val grouped = extractionResult.quads.groupBy(_.predicate)

    val result = grouped.map { case (propertyUri, tuples) =>
      val propertyName = Util.getLastUriComponent(propertyUri)
      val availableYears = reducePredicateQuadsToYears(tuples)
      (propertyName, availableYears.size, availableYears)
    }.toSeq

    PageStats(extractionResult.page.uriTitle, result)
  }

  private def reducePredicateQuadsToYears(quads: List[Quad]) = quads
    .filter(quad => quad.context.get("fromDate").isDefined)
    .map(quad => quad.context.get("fromDate").get.take(4))
    .distinct
    .sorted

  private def round(value: Double) = BigDecimal.valueOf(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble


}
