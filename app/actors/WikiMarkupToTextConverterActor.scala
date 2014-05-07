package actors

import akka.actor.Actor
import reactivemongo.bson.BSONObjectID
import actors.events.EventLogger
import models._
import models.EventTypes._
import play.api.libs.json.Json
import extractors.{WikiMarkupToTextConverter}
import scala.concurrent.Await
import play.api.Logger
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class ConvertRevsFromWikiMarkupToText(extractionRunId: Option[BSONObjectID], number: Int, totalNumber: Int, pageTitleInUri: String)

case class WikiPageNotInCacheException(message: String) extends Exception(message)


class WikiMarkupToTextConverterActor extends Actor {

  def receive = {
    case ConvertRevsFromWikiMarkupToText(extractionRunId, number, totalNumber, pageTitleInUri) => try {
      implicit val _exid = extractionRunId
      convert(pageTitleInUri)
      EventLogger raise Event(convertedResultsToRDF,
        s"($number/$totalNumber) Converted revs of $pageTitleInUri (Actor: ${self.path.name})",
        Json.obj("uriTitle" -> pageTitleInUri))

    } catch {
      case ex: WikiPageNotInCacheException => EventLogger raise Event(wikipageDoesNoExist, s"($number/$totalNumber) Wikipage revisions not in cache: " + pageTitleInUri); None
    }
  }

  private def convert(pageTitleInUri: String) = {
    val revisions = Await.result(Revision.getPageRevs(pageTitleInUri), 5000 milliseconds)
    val revisionsWithPlainText = revisions.map {
      rev =>
        val wikiMarkup = rev.content.get
        val plainText = WikiMarkupToTextConverter.convert(wikiMarkup)
        rev.copy(content = Some(plainText))
    }

    val result = Await.result(TextRevision.saveBulk(revisionsWithPlainText), 5000 milliseconds)
    result.map {
      lastError => lastError.ok match {
        case false => {
          val message = s"$pageTitleInUri: Text Save failed: [lastError=$lastError]"
          Logger.error(message)
          EventLogger raise Event(exception, message)
        }
        case true => // noop
      }
    }
  }

}
