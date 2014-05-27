package actors

import scala.concurrent.duration._
import akka.actor.Actor
import models.{Event, ExtractionRunPageResult, Revision}
import scala.concurrent.Await
import actors.events.EventLogger
import models.EventTypes._
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.api.libs.concurrent.Execution.Implicits._
import org.dbpedia.extraction.wikiparser.WikiParserException
import extraction.extractors.{TemporalDBPediaMappingExtractorWrapper, ThreadUnsafeDependencies}


case class NoRevisionDataException(message: String) extends Exception(message)
case class ExtractInfoboxAndSaveQuads(extractionRunId: BSONObjectID, extractionRunDescription: String, number: Int, totalNumber: Int, pageTitleInUri: String)


class TemporalInfoboxExtractorActor extends Actor {

  val extractor = new TemporalDBPediaMappingExtractorWrapper(ThreadUnsafeDependencies.create(self.path.name))

  override def preStart =  {
    EventLogger raise Event(initializedInfoboxExtractor, s"Initialized Infobox Extractor Actor: ${self.path.name}")
  }

  override def postStop = {
    EventLogger raise Event(stoppedInfoboxExtractor, s"Stopped Infobox Extractor Actor: ${self.path.name}")
  }

  def receive = {
    case ExtractInfoboxAndSaveQuads(extractionRunId, extractionRunDescription, number, totalNumber, pageTitleInUri) =>
      try {
      val runtime = extractAndSafe(extractionRunId, extractionRunDescription, pageTitleInUri)
      logSuccess(extractionRunId, number, totalNumber, pageTitleInUri, runtime)
      }catch{
        case ex: NoRevisionDataException => logNoData(extractionRunId, number, totalNumber, pageTitleInUri)
        case ex: WikiParserException => logWikiParserErrors(extractionRunId, number, totalNumber, pageTitleInUri, ex)
      }
  }

  private def logNoData(extractionRunId: BSONObjectID, number: Int, totalNumber: Int, pageTitleInUri: String) {
    implicit val _extractionRunId = Some(extractionRunId)
    EventLogger raise Event(noRevisionDataFound, s"($number/$totalNumber) No revisions found for page $pageTitleInUri (Actor: ${self.path.name})",
      Json.obj("uriTitle" -> pageTitleInUri))
  }

  private def logWikiParserErrors(extractionRunId: BSONObjectID, number: Int, totalNumber: Int, pageTitleInUri: String, ex: Throwable) {
    implicit val _extractionRunId = Some(extractionRunId)
    EventLogger raise Event(unableToParseWikiContent, s"($number/$totalNumber) Unable to parse Revision Content for page $pageTitleInUri (Actor: ${self.path.name})",
      Json.obj("uriTitle" -> pageTitleInUri, "type" -> ex.getClass.getCanonicalName, "message" -> ex.getMessage))
  }


  private def logSuccess(extractionRunId: BSONObjectID, number: Int, totalNumber: Int, pageTitleInUri: String, runtime: Long) {
    implicit val _extractionRunId = Some(extractionRunId)
    EventLogger raise Event(extractedPageRevisions, s"($number/$totalNumber) Extracted revisions of page $pageTitleInUri (Actor: ${self.path.name}, Runtime $runtime ms)",
      Json.obj("runtime" -> runtime, "uriTitle" -> pageTitleInUri))
  }

  private def extractAndSafe(extractionRunId: BSONObjectID, extractionRunDescription: String, pageTitleInUri: String) =  {
    val revisions = Await.result(Revision.getPageRevs(pageTitleInUri), 5 seconds)
    if(revisions.size == 0){
      throw new NoRevisionDataException("")
    }
    val page = revisions.head.page.get

    val (quads, runtime) = Util.measure( revisions.flatMap( rev => extractor.extract(rev)))

    val pageQuads = ExtractionRunPageResult(extractionRunId, extractionRunDescription, page, quads)
    //Logger.info(pageQuads.toString)
    ExtractionRunPageResult.save(pageQuads).onFailure { case t => throw t}
    runtime
  }
}
