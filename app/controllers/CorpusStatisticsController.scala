package controllers

import actors._
import actors.events.EventLogger
import akka.pattern.ask
import akka.util.Timeout
import models.{Event, ExtractionRun}
import org.slf4j.LoggerFactory
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Controller}
import reactivemongo.bson.BSONObjectID
import models.EventTypes._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


object CorpusStatisticsController extends Controller {

  val log = LoggerFactory.getLogger(getClass)

  def extractWikiTextStatistics(extractionRunId: String) = Action {
    implicit val _exid = Some(BSONObjectID(extractionRunId))
    import actors.DefaultActors._
    implicit val timeout = new Timeout(1 hour)

    log.info(s"Scheduled wiki text statistics for run: $extractionRunId")
    Akka.system.scheduler.scheduleOnce(1 second) {
      log.info(s"Started wiki text statistics for run: $extractionRunId")

      val run = Await.result(ExtractionRun.getById(extractionRunId), 30 seconds).get
      log.info("Loaded Run")

      val openFiles = statisticsSaver ? OpenExtractionRun(extractionRunId)
      Await.result(openFiles, 30 seconds)

      log.info("Collecting statistics")
      val noOfArticles = run.getResources.size
      val factExtractorResults = Future.sequence(
        run.getResources.view.zipWithIndex
          .map { case (pageUri, index) =>
          statisticsExtractor ? CollectStatistics(Some(run.id), index + 1, noOfArticles, pageUri._1)
        })

      factExtractorResults.onComplete {
        case Success(_) =>
          val closeFiles = statisticsSaver ? CloseExtractionRun(extractionRunId)
          Await.result(closeFiles, 5 seconds)

          log.info("Finished stats collection")
          EventLogger raise Event(finishedStatsCollection)
        case Failure(t) =>
          val closeFiles = statisticsSaver ? CloseExtractionRun(extractionRunId)
          Await.result(closeFiles, 5 seconds)

          log.info("Finished stats collection with errors")
          EventLogger raise Event(finishedStatsCollection)
          EventLogger.raiseExceptionEvent(t)
      }
    }
    Ok
  }

}
