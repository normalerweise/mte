package actors

import ch.weisenburger.uima.types.distantsupervision.skala
import ch.weisenburger.uima.types.distantsupervision.skala._
import reactivemongo.bson.BSONObjectID
import akka.actor.Actor
import actors.events.EventLogger
import models.{TextRevision, Event}
import models.EventTypes._
import play.api.libs.json.Json
import scala.concurrent.Await
import scala.concurrent.duration._
import org.slf4j.LoggerFactory
import ch.weisenburger.uima.FinancialDataPipelineFactory

case class ExtractSamplesFromRevisionTexts(extractionRunId: Option[BSONObjectID], number: Int, totalNumber: Int, pageTitleInUri: String)

class SampleFinderActor extends Actor {

  val log = LoggerFactory.getLogger(getClass)

  log.info("creating financial data sample pipeline")
  val sampleFinderPipeline = FinancialDataPipelineFactory.createSampleExctractionScalaCaseClassPipeline
  log.info("created financial data sample pipeline")
  val sampleSaver = DefaultActors.sampleSaver
  val sampleCandidateSaver = DefaultActors.sampleCandidateSaver

  def receive = {
    case ExtractSamplesFromRevisionTexts(extractionRunId, number, totalNumber, pageTitleInUri) => try {
      implicit val _exid = extractionRunId
      val (samples, negativeSamples, sampleCandidates) = extractDistinctSamples(pageTitleInUri)
      sampleSaver ! SaveSamplesOfArticle(samples, sender)
      sampleSaver ! SaveNegativeSamplesOfArticle(negativeSamples, sender)
      sampleCandidateSaver ! SaveSampleCandidatesOfArticle(sampleCandidates, sender)


      EventLogger raise Event(extractedSamplesFromPageRevisions,
        s"($number/$totalNumber) Extracted ${samples.size} samples from revs of $pageTitleInUri (Actor: ${self.path.name})",
        Json.obj("uriTitle" -> pageTitleInUri, "noOfSamples" -> samples.size, "noOfSampleCandidates" -> sampleCandidates.size))

    } catch {
      case ex: WikiPageNotInCacheException =>
        EventLogger raise Event(wikipageDoesNoExist, s"($number/$totalNumber) Wikipage revisions not in cache: " + pageTitleInUri)
    }
  }

  private def extractDistinctSamples(pageTitleInUri: String): (Seq[Sample], Seq[NegativeSample], Seq[SampleCandidate]) = {
    log.info("processing " + pageTitleInUri)
    val revisions = Await.result(TextRevision.getPageRevs(pageTitleInUri), 30 seconds)

    val (samples, negativeSamples, sampleCandidates) = revisions.map { rev =>
      val wikiText = rev.content.get
      val dbpediaResourceName = rev.page.get.dbpediaResourceName
      val dbpediaResourceURI =  s"http://dbpedia.org/resource/${dbpediaResourceName}"
      val wikiRevId = rev.id
      val wikiArticleName = rev.page.get.wikipediaArticleName

      log.trace("processing " + dbpediaResourceName + " " + wikiRevId)

      val poNegSamplesAndSampleCandidates = sampleFinderPipeline
        .process(wikiText, dbpediaResourceURI, wikiRevId, wikiArticleName)


      if(log.isTraceEnabled) {
        log.trace(s"$wikiArticleName: $wikiRevId:" +poNegSamplesAndSampleCandidates._1.size)
      }
      poNegSamplesAndSampleCandidates
    }
    .foldLeft((List.empty[Sample], List.empty[NegativeSample], List.empty[SampleCandidate])) { case (aggregate, sAndC) =>
      (aggregate._1 ++ sAndC._1, aggregate._2 ++ sAndC._2, aggregate._3 ++ sAndC._3)
    }

    val distinctSamples = samples.groupBy(s => s.sentenceText.replaceAll("\\s","") + s.quad).map { case (_, equalSamples) =>
        val revisions = equalSamples.flatMap(_.revisionNumber)
        equalSamples.head.copy(revisionNumber = revisions)
    }

    val distinctSampleCandidates = sampleCandidates.groupBy(_.sentenceText.replaceAll("\\s","")).map { case (_, equalSampleCandidates) =>

        val agg = equalSampleCandidates.foldLeft((Set.empty[Entity], Set.empty[Relation], Set.empty[skala.Value], Set.empty[Timex])) {
          case (aggregate, candidate) =>
            (aggregate._1 ++ candidate.entities, aggregate._2 ++ candidate.relations,
              aggregate._3 ++ candidate.values, aggregate._4 ++ candidate.timexes)
        }

        equalSampleCandidates.head.copy(
          entities = agg._1.toSeq,
          relations = agg._2.toSeq,
          values = agg._3.toSeq,
          timexes = agg._4.toSeq)
    }

    log.trace ("processed " + pageTitleInUri)
    (distinctSamples.toSeq, negativeSamples, distinctSampleCandidates.toSeq)
  }

}