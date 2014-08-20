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
import extraction.extractors.{TemporalDBPediaMappingExtractorWrapper, Dependencies}
import org.github.jamm.MemoryMeter
import org.slf4j.LoggerFactory


case class NoRevisionDataException(message: String) extends Exception(message)
case class ExtractInfoboxAndSaveQuads(extractionRunId: BSONObjectID, extractionRunDescription: String, number: Int, totalNumber: Int, pageTitleInUri: String)


class TemporalInfoboxExtractorActor extends Actor {

  val logger = LoggerFactory.getLogger(getClass)

  val dependencies = new Dependencies()
  val extractor = new TemporalDBPediaMappingExtractorWrapper(dependencies)



  override def preStart =  {
    EventLogger raise Event(initializedInfoboxExtractor, s"Initialized Infobox Extractor Actor: ${self.path.name}")
  }

  override def preRestart(reason: Throwable, message: Option[Any]) =  {
    EventLogger raise Event(restartedInfoboxExtractor,
      s"Restarted Infobox Extractor Actor ${self.path.name}: ${message.getOrElse("")}")
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
    logger.debug(s"Load revisions of $pageTitleInUri")
    val revisions = Await.result(Revision.getPageRevs(pageTitleInUri), 30 seconds)
    if(revisions.size == 0){
      throw new NoRevisionDataException("")
    }
    val page = revisions.head.page.get
    val (quads, runtime) = Util.measure( revisions.flatMap { rev =>
      logger.debug(s"Extracting revision ${rev.id} resource ${rev.page.get.dbpediaResourceName}")
      extractor.extract(rev)
    })

    val pageQuads = ExtractionRunPageResult(extractionRunId, extractionRunDescription, page, quads)
    //Logger.info(pageQuads.toString)
    ExtractionRunPageResult.save(pageQuads).onFailure { case t => throw t}
    logger.debug(s"Finished and Saved page results of $pageTitleInUri")
    runtime
  }

  context.system.scheduler.schedule(10 seconds, 30 minutes) {
    reportExtractorMemoryUsage()
  }

  var meterIsInstrumented = true
  val meter = new MemoryMeter()
  val memoryLogger =  LoggerFactory.getLogger("TemporalInfoboxExtractorActorMemory")
  private def reportExtractorMemoryUsage() = try {
    if (meterIsInstrumented && memoryLogger.isDebugEnabled) {
      memoryLogger.debug(s"""
      | TIE ${self.path.name} extractor: ${meter.measure(extractor)/1024 } kb;
      | TIE ${self.path.name} extractor deep: ${meter.measureDeep(extractor)/1024} kb;
      | TIE ${self.path.name} timex parser: ${meter.measure(dependencies.advancedTimexParser)/1024 } kb;
      | TIE ${self.path.name} timex parser deep: ${meter.measureDeep(dependencies.advancedTimexParser)/1024} kb;
      """.stripMargin)
    }
  }catch {
    case ex: java.lang.IllegalStateException =>
      meterIsInstrumented = false
      memoryLogger.debug(ex.getMessage)
  }
}
