package actors

import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.Actor
import extractors.{WikiPageDoesNotExistException, RelevantRevisionDownloader}
import models.{Event, Revision}
import models.EventTypes._
import actors.events.EventLogger
import play.api.Logger
import scala.util.{Failure, Success}
import actors.DownloadAndSavePage
import extractors.WikiPageDoesNotExistException
import play.api.libs.json.Json

case class DownloadAndSavePage(pageTitleInUri: String)

class PageDownloaderActor extends Actor {

  def receive = {
    case DownloadAndSavePage(pageTitleInUri) => downloadAndSavePage(pageTitleInUri)
  }

  private def downloadAndSavePage(pageTitleInUri: String) = try {
    val (revisions, runtime) = measure(RelevantRevisionDownloader.download(pageTitleInUri))
    Revision.saveBulk(revisions).map(_.map {
      lastError => lastError.inError match {
        case true => {
          val message = s"$pageTitleInUri: Save failed: [lastError=$lastError]"
          Logger.error(message)
          EventLogger raise Event(exception,message)
        }
        case false => Unit
      }
    } recover {
      case ex =>
        val message = s"$pageTitleInUri: Save failed: [message=${ex.getMessage}]"
        Logger.error(message)
        EventLogger raise Event(exception, message)
    })
    EventLogger raise Event(downloadedPageRevisions, s"Downloaded revs of $pageTitleInUri (Actor: ${self.path.name}, Runtime $runtime ms)",
      Json.obj("runtime" -> runtime, "uriTitle" -> pageTitleInUri) )
  } catch {
    case ex: WikiPageDoesNotExistException => EventLogger raise Event(wikipageDoesNoExist, "Wikipage does not exist: " + pageTitleInUri)
  }

  private def measure[T](f: => T) = {
    val start = System.currentTimeMillis
    val result = f
    val end = System.currentTimeMillis
    (result, end - start)
  }
}
