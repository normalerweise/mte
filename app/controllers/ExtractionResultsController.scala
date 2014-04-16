/**
 * Created by Norman on 20.03.14.
 */
package controllers

import play.api.mvc.{Action, Controller}
import java.io.File
import scala.concurrent.future
import play.api.libs.json._
import scala.io.{Codec, Source}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger
import java.nio.charset.MalformedInputException
import models.{Event, Util, ExtractionRunPageResult, Quad}
import scala.math.BigDecimal.RoundingMode


object ExtractionResultsController extends Controller {


  import play.api.libs.functional.syntax._
  import play.api.libs.json._


  implicit val codec = Codec.UTF8

  case class PageStats(pageName: String, properties: Seq[(String,Int,List[String])])

  implicit val quadReads: Reads[Quad] = (
    (JsPath \ "subject").read[String] and
    (JsPath \ "predicate").read[String] and
    (JsPath \ "object").read[String] and
    (JsPath \ "context").read[Map[String,String]]
    )(Quad.apply _)

  implicit val pageStatsWrites = new Writes[PageStats] {
    def writes(pageStats: PageStats) = {
      val props = pageStats.properties.map( prop => prop._1 -> Json.toJson(Json.obj("numberOfYears" -> prop._2, "years" -> prop._3)))
      val name = Seq(("pageName" -> Json.toJson(pageStats.pageName)))
      val res = name ++ props
      Json.toJson(res.toMap)
    }
  }

  def getExtractionRunEventStats(extractionRunId: String) = Action.async {
      Event.eventStatsForRun(extractionRunId).map( res  => Ok(Json.prettyPrint(Json.toJson(res))))
  }


  def getExtractionRunStats(extractionRunId: String) = Action.async {
    val run = ExtractionRunPageResult.get(extractionRunId)

     val statsPerPage = run.map(_.par.map( quadToStats(_)))

     val aggregatesPerProperty = statsPerPage.map( stats =>
     stats.flatMap(_.properties).groupBy(_._1).toSeq.map { stat =>
       val propertyName = stat._1
       val size = stat._2.size.toDouble
       val _3years = stat._2.filter(_._2 >= 3)
       val _5years = _3years.filter(_._2 >= 5)

       val _3yearsRel = round(_3years.size / size)
       val _5yearsRel = round(_5years.size / size)

       (propertyName, Json.obj("_3years" -> _3years.size, "_3yearsRel" -> _3yearsRel, "_5years" -> _5years.size, "_5yearsRel" -> _5yearsRel)) } )

     val result = for {
       perPage <- statsPerPage
       aggregates <- aggregatesPerProperty
     } yield Json.obj(
         "noOfPages" -> perPage.size,
         "aggregated" -> Json.toJson(aggregates.toMap.seq),
         "perPage" -> Json.toJson(perPage.toList))

    result.map( res => Ok(Json.prettyPrint(res)))
  }

  def listExtractionRunResults(extractionRunId: String) = Action.async {
      ExtractionRunPageResult.listAsJson(extractionRunId).map( results => Ok(Json.toJson(results)))
  }

  def getExtractionRunResults(extractionRunId: String) = Action.async {
    ExtractionRunPageResult.getAsJson(extractionRunId).map( results => Ok(Json.toJson(results)))
  }

  def getExtractionRunResultsOfPage(extractionRunId: String, pageTitle: String) = Action.async {
    ExtractionRunPageResult.getAsJson(extractionRunId, pageTitle).map( results => Ok(Json.prettyPrint(Json.toJson(results))))
  }

  def getAll = Action.async {
    //future { Ok("test")}
   ExtractionRunPageResult.getAllAsJson.map( results => Ok(Json.toJson(results)))
  }

  private def quadToStats(extractionResult: ExtractionRunPageResult) = {
    val grouped = extractionResult.quads.groupBy(_.predicate)

    val result = grouped.map{ case (propertyUri, tuples) =>
      val propertyName = Util.getLastUriComponent(propertyUri)
      val availableYears = reducePredicateQuadsToYears(tuples)
      (propertyName, availableYears.size, availableYears )}.toSeq

    PageStats(extractionResult.page.uriTitle, result)
  }

  private def reducePredicateQuadsToYears(quads: List[Quad]) = quads
    .filter( quad => quad.context.get("fromDate").isDefined)
    .map( quad => quad.context.get("fromDate").get.take(4))
    .distinct
    .sorted

  private def round(value: Double) = BigDecimal.valueOf(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble


}
