/**
 * Created by Norman on 20.03.14.
 */
package controllers

import org.slf4j.LoggerFactory
import play.api.libs.iteratee.Iteratee
import play.api.mvc.{Action, Controller}
import scala.io.Codec
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models._

import play.api.libs.concurrent.Akka
import actors.events.EventLogger
import models.EventTypes._
import models.Quad
import play.api.Play.current
import extraction.formatters.{SingletonPropertyTurtleSaver, QuadsMerger}

import scala.util.{Failure,Success}


object ExtractionResultsController extends Controller {

  import play.api.libs.json._

  val logger = LoggerFactory.getLogger(getClass)

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

  def distinctQuadsToRDF(extractionRunId: String) = Action {
    Akka.system.scheduler.scheduleOnce(100 microseconds) {
      logger.debug("Starting distinct quads To RDF Conversion")
      val turtleSaver =  new SingletonPropertyTurtleSaver(s"data/$extractionRunId-distinct.tt")
      val enumerator = ExtractionRunPageResult.getEnumerator(extractionRunId)
      val consume = Iteratee.foreach[ExtractionRunPageResult] { res =>
        //val results = mergeDistinct(res)
        val results = mergeDistinctWithFilter(res)
        turtleSaver.write(results, res.page.wikipediaArticleName)
      }
      logger.debug("Start merging distinct quads")
      val eventualResult = enumerator(consume)


      eventualResult.onComplete {
        case Failure(t) => EventLogger raiseExceptionEvent(t)
        case Success(pageExtractionResults) =>
          turtleSaver.close
          logger.debug("Saved quads")
          EventLogger raise Event(convertedResultsToRDF, s"Converted Results to distinct RDF for Run: ${extractionRunId}")
      }
    }
    Ok
  }

  def quadsToRDF(extractionRunId: String) = Action {
    Akka.system.scheduler.scheduleOnce(100 microseconds) {
      logger.debug("Starting quads To RDF Conversion")
      val turtleSaver =  new SingletonPropertyTurtleSaver(s"data/$extractionRunId.tt")
      val enumerator = ExtractionRunPageResult.getEnumerator(extractionRunId)
      val consume = Iteratee.foreach[ExtractionRunPageResult] { res =>
        val results = mergeValue(res)
        turtleSaver.write(results, res.page.wikipediaArticleName)
      }
      logger.debug("Start merging quads")
      val eventualResult = enumerator(consume)


      eventualResult.onComplete {
        case Failure(t) => EventLogger raiseExceptionEvent(t)
        case Success(pageExtractionResults) =>
          turtleSaver.close
          logger.debug("Saved quads")
          EventLogger raise Event(convertedResultsToRDF, s"Converted Results to RDF for Run: ${extractionRunId}")
      }
    }
    Ok
  }

  private val mergeDistinct: (ExtractionRunPageResult)=>Seq[Quad] = (res) => {
    // TODO: For now year precision is sufficient
    val quadsWithYearPrecision = res.quads.map(convertToYearPrecision)
    QuadsMerger.getDistinctQuadsPerYear(quadsWithYearPrecision)
  }

  private val mergeValue: (ExtractionRunPageResult)=>Seq[Quad] = (res) => {
    // TODO: For now year precision is sufficient
    val quadsWithYearPrecision = res.quads.map(convertToYearPrecision)
    QuadsMerger.getDistinctQuadsPerValueAndTimex(quadsWithYearPrecision)
  }

  private val mergeDistinctWithFilter: (ExtractionRunPageResult)=>Seq[Quad] = (res) => {
    // TODO: For now year precision is sufficient
    val quadsWithYearPrecision = res.quads.map(convertToYearPrecision)
    QuadsMerger.getDistinctQuadsPerYearWithNonTemporalFilter(quadsWithYearPrecision)
  }

//  private val enRes = "^http:\\/\\/en.dbpedia.org\\/resource\\/(.*)$".r
//
//  /** The extractor returns resource quads with the english resource uri
//   *  In order to compare with dbpedia.org sparql endpoint
//   * @return resources with dpedia.org/resource as subject uri
//   */
//  private def transformResourceUri: (Seq[Quad])=>Seq[Quad] = (qs) => qs.map( q => q.subject match {
//    case enRes(title) => q.copy(subject = s"http://dbpedia.org/resource/$title")
//    case _ => q
//  })



  val convertToYearPrecision = (q: Quad) => {
    val from = q.fromDate match {
      case Some(year) => Seq("fromDate" -> year.substring(0, 4))
      case None => Seq.empty
    }
    val to = q.toDate match {
      case Some(year) => Seq("toDate" -> year.substring(0, 4))
      case None => Seq.empty
    }
    val newQ = q.copy(context = q.context ++ from ++ to)
    newQ
  }

  def getResultsAsRDF(extractionRunId: String) = Application.dataFile(extractionRunId + ".tt")
  def getResultsAsDistinctRDF(extractionRunId: String) = Application.dataFile(extractionRunId + "-distinct.tt")


  private def quadToStats(extractionResult: ExtractionRunPageResult) = {
    val grouped = extractionResult.quads.groupBy(_.predicate)

    val result = grouped.map { case (propertyUri, tuples) =>
      val propertyName = Util.getLastUriComponent(propertyUri)
      val availableYears = reducePredicateQuadsToYears(tuples)
      (propertyName, availableYears.size, availableYears)
    }.toSeq

    PageStats(extractionResult.page.dbpediaResourceName, result)
  }

  private def reducePredicateQuadsToYears(quads: List[Quad]) = quads
    .filter(quad => quad.context.get("fromDate").isDefined)
    .map(quad => quad.context.get("fromDate").get.take(4))
    .distinct
    .sorted

  private def round(value: Double) = BigDecimal.valueOf(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble


}
