/**
 * Created by Norman on 20.03.14.
 */
package controllers

import actors.events.EventLogger
import extraction.OntologyUtil
import extraction.formatters.{QuadsMerger, SingletonPropertyTurtleSaver}
import models.EventTypes._
import models.{Quad, _}
import org.slf4j.LoggerFactory
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Iteratee
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}


object ExtractionResultsController extends Controller {

  private val logger = LoggerFactory.getLogger(getClass)

  private def temporallyConsistentDataSetFilePath(implicit extractionRunId: String) =
    s"data/$extractionRunId-temporally_consistent.tt"

  private def distinctFactDataSetFilePath(implicit extractionRunId: String) =
    s"data/$extractionRunId-distinct_fact.tt"


  def listExtractionRunResults(extractionRunId: String) = Action.async {
    ExtractionRunPageResult.
      listAsJson(extractionRunId)
      .map(results =>
      Ok(Json.toJson(results)))
  }

  def getExtractionRunResults(extractionRunId: String) = Action.async {
    ExtractionRunPageResult
      .getAsJson(extractionRunId)
      .map(results =>
      Ok(Json.toJson(results)))
  }

  def getExtractionRunResultsOfResource(extractionRunId: String, resourceName: String) = Action.async {
    ExtractionRunPageResult
      .getAsJson(extractionRunId, resourceName)
      .map(results =>
      Ok(Json.prettyPrint(Json.toJson(results))))
  }


  def mergeExtractedQuadsToTemporallyConsistentRDFDataSet(implicit extractionRunId: String) = Action {

    val mergeFunction = (quads: Seq[Quad]) => {
      QuadsMerger.mergeTemporallyConsistent(quads)
    }

    scheduleDataSetGeneration(
      extractionRunId,
      dataSetName = "temporally consistent",
      dataSetFilePath = temporallyConsistentDataSetFilePath,
      mergeFunction
    )
    Ok
  }

  def mergeExtractedQuadsToDistinctRDFDataSet(implicit extractionRunId: String) = Action {

    val mergeFunction = (quads: Seq[Quad]) => {
      QuadsMerger.getDistinctQuadsPerValueAndTemporalInformation(quads)
    }

    scheduleDataSetGeneration(
      extractionRunId,
      dataSetName = "distinct fact",
      dataSetFilePath = distinctFactDataSetFilePath,
      mergeFunction
    )
    Ok
  }


  def serveDistinctFactDataSet(implicit extractionRunId: String) = Action {
    Application.sendFile(distinctFactDataSetFilePath)
  }


  def serveTemporallyConsistentDataSet(implicit extractionRunId: String) = Action {
    Application.sendFile(temporallyConsistentDataSetFilePath)
  }




  private def scheduleDataSetGeneration(
                                         extractionRunId: String,
                                         dataSetName: String,
                                         dataSetFilePath: String,
                                         mergeFunction: Seq[Quad] => Seq[Quad]) =
    Akka.system.scheduler.scheduleOnce(100 microseconds) {
      logger.debug(s"Started $dataSetName RDF data set generation")
      val enumerator = ExtractionRunPageResult.getEnumerator(extractionRunId)
      val turtleSaver = new SingletonPropertyTurtleSaver(dataSetFilePath)
      val selectAndWriteQuads = Iteratee.foreach[ExtractionRunPageResult]{ res =>
        // Note: Filter selected relations -> Don't include non temporally annotated properties
        // Note: For now year precision for dates is sufficient -> Simplifies the merge algorithm
        val quadsWithYearPrecision = res.quads filter selectedRelations map toYearPrecision
        val selectedQuads = mergeFunction(quadsWithYearPrecision)
        turtleSaver.write(selectedQuads, res.page.wikipediaArticleName)
      }
      logger.debug("Start merging quads")
      enumerator(selectAndWriteQuads).onComplete {
        case Failure(t) =>
          turtleSaver.close
          EventLogger raiseExceptionEvent (t)

        case Success(s) =>
          turtleSaver.close
          logger.debug("Saved quads")
          EventLogger raise Event(convertedResultsToRDF,
            s"Converted Extracted Quads to $dataSetName RDF Data Set for Run: $extractionRunId")
      }
    }

  private val toYearPrecision = (q: Quad) => {
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

  private val selectedRelations = (q: Quad) =>
    OntologyUtil.isTemporal1to1Predicate(q.predicate) || OntologyUtil.isOntologyPredicate(q.predicate)

}

