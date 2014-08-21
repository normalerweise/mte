package actors

import actors.events.EventLogger
import akka.actor.Actor
import ch.weisenburger.uima.FinancialDataPipelineFactory
import ch.weisenburger.uima.types.distantsupervision.skala.{ExtractedFact, NegativeSample, Sample}
import models.{TextRevision, Event}
import models.EventTypes._
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import scala.concurrent.duration._

import scala.concurrent.Await

case class ExtractFactsFromRevisionTexts(extractionRunId: Option[BSONObjectID], number: Int, totalNumber: Int, pageTitleInUri: String)




class FreeTextFactExtractionActor extends Actor {



    val log = LoggerFactory.getLogger(getClass)

    log.info("creating financial data fact extraction pipeline")
    val factExtractionPipeline = FinancialDataPipelineFactory.createFactExctractionScalaCaseClassPipeline
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
              EventLogger raise Event(wikipageDoesNoExist, s"($number/$totalNumber) Wikipage revisions not in cache: " + pageTitleInUri)
          }
        }

    private def extractDistinctFacts(pageTitleInUri: String): Seq[ExtractedFact] = {
      log.info("processing " + pageTitleInUri)
      val revisions = Await.result(TextRevision.getPageRevs(pageTitleInUri), 30 seconds)

      val allFacts = revisions.flatMap { rev =>
        val wikiText = rev.content.get
        val dbpediaResourceName = rev.page.get.dbpediaResourceName
        val dbpediaResourceURI = s"http://dbpedia.org/resource/${dbpediaResourceName}"
        val wikiRevId = rev.id
        val wikiArticleName = rev.page.get.wikipediaArticleName

        log.trace("processing " + dbpediaResourceName + " " + wikiRevId)

        val extractedFacts = factExtractionPipeline
          .process(wikiText, dbpediaResourceURI, wikiRevId, wikiArticleName)

        if (log.isTraceEnabled) {
          log.trace(s"$wikiArticleName: $wikiRevId: " + extractedFacts.size)
        }

        extractedFacts
      }
      //TODO: aggregate revisions
      .groupBy(f => f.sentenceText + f.quad.relation)
      .map { case (_, facts) => facts.head }
      .toSeq

      log.trace ("processed " + pageTitleInUri)
      allFacts
    }
}
