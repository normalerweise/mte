package actors

import actors.events.EventLogger
import akka.actor.{Actor, Status}
import ch.weisenburger.uima.FinancialDataPipelineFactory
import ch.weisenburger.uima.types.distantsupervision.skala.ExtractedFact
import models.Event
import models.EventTypes._
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID

case class ExtractFactsFromRevisionTexts(extractionRunId: Option[BSONObjectID], number: Int, totalNumber: Int, pageTitleInUri: String)


class FreeTextFactExtractionActor extends Actor with RevisionsOfResourceProcessor {

  val log = LoggerFactory.getLogger(getClass)

  log.info("creating financial data fact extraction pipeline")
  var factExtractionPipeline = FinancialDataPipelineFactory.createFactExctractionScalaCaseClassPipeline
  log.info("created financial data fact extraction pipeline")
  val factSaver = DefaultActors.factSaver


  def receive = {
    case ExtractFactsFromRevisionTexts(extractionRunId, number, totalNumber, pageTitleInUri) => try {
      implicit val _exid = extractionRunId
      val facts = extractDistinctFacts(pageTitleInUri)
      factSaver ! SaveExtractedFactsOfArticle(facts, sender)

      EventLogger raise Event(extractedFactsFromPageRevisions,
        s"($number/$totalNumber) Extracted ${facts.size} facts from revs of $pageTitleInUri (Actor: ${self.path.name})",
        Json.obj("uriTitle" -> pageTitleInUri, "noOfFacts" -> facts.size))

    } catch {
      case ex: WikiPageNotInCacheException =>
        sender ! Status.Failure(ex)
        EventLogger raise Event(wikipageDoesNoExist, s"($number/$totalNumber) Wikipage revisions not in cache: " + pageTitleInUri)
        sender ! Status.Success("no_wiki_data")

      case e: RevisionsOfResourceProcessingOutOfMemory =>
        log.error("freeing fact extraction pipeline")
        factExtractionPipeline = null
        Util.gc
        log.error("finished gc go on")
        sender ! Status.Success("error")
        throw e

      case e: Exception =>
        sender ! Status.Success("error")
        throw e
    }
  }

  private def extractDistinctFacts(pageTitleInUri: String): Seq[ExtractedFact] = {

    val allFacts = processText(pageTitleInUri) { rev =>
      val extractedFacts = factExtractionPipeline
        .process(rev.content, rev.resourceURI, rev.wikipediaRevId, rev.wikipediaArticleName)

      if (log.isTraceEnabled) {
        log.trace(s"${rev.wikipediaArticleName}: ${rev.wikipediaRevId}: " + extractedFacts.size)
      }

      extractedFacts
    }

    //TODO: aggregate revisions
    val agg = allFacts.flatten.groupBy(f => f.sentenceText + f.quad.relation)
      .map { case (_, facts) => facts.head}
      .toSeq

    log.info(s"$pageTitleInUri found ${agg.size} facts")

    agg
  }
}
