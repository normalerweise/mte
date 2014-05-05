package actors

import scala.util.{Success, Failure}
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.Actor
import extractors.RelevantRevisionDownloader
import models.{Event, Revision}
import models.EventTypes._
import actors.events.EventLogger
import play.api.Logger
import extractors.WikiPageDoesNotExistException
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import scala.concurrent.Await

case class DownloadAndSavePage(extractionRunId: Option[BSONObjectID], number: Int, totalNumber: Int, pageTitleInUri: String)

class PageDownloaderActor extends Actor {

  def receive = {
    case DownloadAndSavePage(extractionRunId, number, totalNumber, pageTitleInUri) => try {
      implicit val _extractionRunId = extractionRunId
      downloadAndSavePage(pageTitleInUri) match {
        case Some(runtime) => EventLogger raise Event(downloadedPageRevisions, s"($number/$totalNumber) Downloaded revs of $pageTitleInUri (Actor: ${self.path.name}, Runtime $runtime ms)",
          Json.obj("runtime" -> runtime, "uriTitle" -> pageTitleInUri))
        case _ => Unit
      }
    } catch {
      case ex: WikiPageDoesNotExistException => EventLogger raise Event(wikipageDoesNoExist, s"($number/$totalNumber) Wikipage does not exist: " + pageTitleInUri); None
    }
  }

  private def downloadAndSavePage(pageTitleInUri: String)(implicit extractionRunId: Option[BSONObjectID]) = {
    val (revisions, runtime) = Util.measure(RelevantRevisionDownloader.download(pageTitleInUri))
    Logger.info(revisions.sortBy(_.timestamp.toDate).reverse.take(5).map(_.timestamp).mkString("\n"))
//    Await.result(Revision.saveBulk(revisions).map(_.onComplete {
//      case Success(lastError) => lastError.ok match {
//        case false => {
//          val message = s"$pageTitleInUri: Save failed: [lastError=$lastError]"
//          Logger.error(message)
//          EventLogger raise Event(exception, message)
//        }
//        case true => Logger.info("completed " + lastError)
//      }
//      case Failure(ex) => val message = s"$pageTitleInUri: Save failed: [message=${ex.getMessage}]"
//        Logger.error(message)
//        EventLogger raise Event(exception, message)
//    }), 5000 milliseconds)


    import scala.concurrent.duration._
    val result = Await.result(Revision.saveBulk(revisions), 5000 milliseconds)
     result.map { lastError => lastError.ok match {
      case false => {
        val message = s"$pageTitleInUri: Save failed: [lastError=$lastError]"
        Logger.error(message)
        EventLogger raise Event(exception, message)
      }
      case true => Logger.info("completed " + lastError)
     }}
    Some(runtime)
  }

}
