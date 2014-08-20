package controllers

import actors._
import actors.events.EventLogger
import akka.pattern.ask
import akka.util.Timeout
import models.EventTypes._
import models.{Event, ExtractionRun}
import org.slf4j.LoggerFactory
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Controller}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


/**
 * Created by Norman on 20.03.14.
 */
object FreeTextExtractionController extends Controller {

  val log = LoggerFactory.getLogger(getClass)

  def extractFactsFromWikiText(extractionRunId: String) = Action {
    implicit val _exid = Some(new BSONObjectID(extractionRunId))
    import actors.DefaultActors._
    implicit val timeout = new Timeout(1 hour)

    log.info(s"Scheduled fact extraction for run: $extractionRunId")
    Akka.system.scheduler.scheduleOnce(1 second) {
      log.info(s"Started fact extraction for run: $extractionRunId")

      val run = Await.result(ExtractionRun.getById(extractionRunId), 30 seconds).get
      log.info("Loaded Run")

      val openFiles = factSaver ? OpenExtractionRun(extractionRunId)
      Await.result(openFiles, 30 seconds)

      log.info("Extracting samples")
      val noOfArticles = run.getResources.size
      val factExtractorResults = Future.sequence(
        run.getResources.view.zipWithIndex
          .map { case (pageUri, index) =>
          factExtractor ? ExtractFactsFromRevisionTexts(Some(run.id), index + 1, noOfArticles, pageUri._1)
        })

      Await.result(factExtractorResults, 48 hours)

      val closeFiles = factSaver ? CloseExtractionRun(extractionRunId)
      Await.result(closeFiles, 5 seconds)

      log.info("Finished fact Extraction")
      EventLogger raise Event(finishedFactExtraction)
    }
    Ok
  }

}
