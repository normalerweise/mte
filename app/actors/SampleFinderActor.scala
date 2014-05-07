package actors

import reactivemongo.bson.BSONObjectID
import akka.actor.Actor
import actors.events.EventLogger
import models.{TextRevision, Event}
import models.EventTypes._
import scala.Some
import play.api.libs.json.Json
import scala.concurrent.Await
import scala.concurrent.duration._
import ner.LingPipeTrainingGenerator
import play.api.Logger
import org.slf4j.LoggerFactory


case class ExtractSamplesFromRevisionTexts(extractionRunId: Option[BSONObjectID], number: Int, totalNumber: Int, pageTitleInUri: String, predicates: List[String])

class SampleFinderActor extends Actor {

  val logger = LoggerFactory.getLogger(this.getClass);

  def receive = {
    case ExtractSamplesFromRevisionTexts(extractionRunId, number, totalNumber, pageTitleInUri, predicates) => try {
      implicit val _exid = extractionRunId
      val samples = extract(pageTitleInUri)
      sender ! samples

      if(logger.isInfoEnabled && samples.size > 0) {
        logger.info(s"Extracted ${samples.size} samples from $pageTitleInUri revs")
      }

      EventLogger raise Event(extractedSamplesFromPageRevisions,
        s"($number/$totalNumber) Extracted ${samples.size} samples from revs of $pageTitleInUri (Actor: ${self.path.name})",
        Json.obj("uriTitle" -> pageTitleInUri, "noOfSamples" -> samples.size))

    } catch {
      case ex: WikiPageNotInCacheException => EventLogger raise Event(wikipageDoesNoExist, s"($number/$totalNumber) Wikipage revisions not in cache: " + pageTitleInUri); None
    }
  }

  private def extract(pageTitleInUri: String) = {
    val revisions = Await.result(TextRevision.getPageRevs(pageTitleInUri), 10000 milliseconds)
    val samples = revisions.flatMap {
      rev =>
        val wikiText = rev.content.get
        LingPipeTrainingGenerator.findSamplesInRevision(wikiText, rev.id.toString, rev.page.get.id.toString)
    }
   samples
  }

}