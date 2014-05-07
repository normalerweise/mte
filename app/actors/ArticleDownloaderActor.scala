package actors

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.{ActorLogging, Actor}
import actors.events.EventLogger
import reactivemongo.bson.BSONObjectID
import monitoring.Instrumented
import play.api.libs.json.Json
import models.{Revision, Event}
import models.EventTypes._
import extractors.download.{WikipediaClient, WikipediaRevisionSelector}
import extractors.download.WikipediaClient.WikiPageDoesNotExistException


// Commands for the article downloader actor
case class DownloadAndSaveArticle(extractionRunId: Option[BSONObjectID], number: Int,
                                  totalNumber: Int, articleNameInUrl: String)


/** Select, Download and Store Wikipedia Revisions of articles
  *
  * Actor wraps the tasks of selecting interesting Wikipedia revisions
  *  (revisions with as much different temporal triples as possible),
  *  downloading those,
  *  and storing them in the revision cache
  *
  * Actor model serves parallelization and resilience against exceptions
  *
  */
class ArticleDownloaderActor extends Actor with ActorLogging with Instrumented {

  def receive = {
    case DownloadAndSaveArticle(extractionRunId, number, totalNumber, articleNameInUrl) => try {
      implicit val _extractionRunId = extractionRunId

      val das = metrics.timer("download_and_save").timerContext
      downloadAndSaveArticle(articleNameInUrl)
      val runtime = (das.stop nanoseconds).toMillis

      EventLogger raise Event(downloadedPageRevisions,
        s"($number/$totalNumber) Downloaded revs of $articleNameInUrl (Actor: ${self.path.name}, Runtime $runtime ms)",
          Json.obj("runtime" -> runtime, "uriTitle" -> articleNameInUrl))

    } catch {
      case ex: WikiPageDoesNotExistException =>
        EventLogger raise Event(wikipageDoesNoExist,
          s"($number/$totalNumber) Wikipage does not exist: " + articleNameInUrl); None
    }
  }

  private def downloadAndSaveArticle(articleNameInUrl: String)(implicit extractionRunId: Option[BSONObjectID]) = try {

    val en = metrics.timer("enumerate_and_select_revisions")
    val dl = metrics.timer("download_revision_contents")
    val st = metrics.timer("store_revision_contents_to_mongo")

    val revisions = en.time {
      WikipediaRevisionSelector.getRevisionsForExtraction(articleNameInUrl,
        WikipediaRevisionSelector.revisionsAtQuartilesPlusLatestForEachYear)
    }

    val revisionsWithContent = dl.time {
      WikipediaClient.downloadRevisionContents(revisions, articleNameInUrl)
    }

    val result = st.time {
      Await.result(Revision.deleteAllRevisionsOf(articleNameInUrl), 5 seconds)
      Await.result(Revision.saveBulk(revisionsWithContent), 5 seconds)
    }
    result.map { lastError => lastError.ok match {
      case false => {
        val message = s"$articleNameInUrl: Save failed: [lastError=$lastError]"
        log.error(message)
        EventLogger raise Event(exception, message)
      }
      case true => // noop
    }
    }

  }

}
