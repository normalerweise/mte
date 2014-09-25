package actors

import actors.events.EventLogger
import akka.actor.{Actor, Status}
import ch.weisenburger.uima.FinancialDataPipelineFactory
import ch.weisenburger.uima.types.distantsupervision.skala
import ch.weisenburger.uima.types.distantsupervision.skala._
import models.Event
import models.EventTypes._
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID

case class ExtractSamplesFromRevisionTexts(extractionRunId: Option[BSONObjectID], number: Int, totalNumber: Int, pageTitleInUri: String)

class SampleFinderActor extends Actor with RevisionsOfResourceProcessor {

  val log = LoggerFactory.getLogger(getClass)

  log.info("creating financial data sample pipeline")
  var sampleFinderPipeline = FinancialDataPipelineFactory.createSampleExctractionScalaCaseClassPipeline
  log.info("created financial data sample pipeline")
  val sampleSaver = DefaultActors.sampleSaver
  val sampleCandidateSaver = DefaultActors.sampleCandidateSaver

  def receive = {
    case ExtractSamplesFromRevisionTexts(extractionRunId, number, totalNumber, pageTitleInUri) => try {
      implicit val _exid = extractionRunId
      val (samples, negativeSamples, sampleCandidates) = extractDistinctSamples(pageTitleInUri)
      sampleSaver ! SavePositiveSamplesOfArticle(samples)
      sampleSaver ! SaveNegativeSamplesOfArticle(negativeSamples)
      sampleCandidateSaver ! SavePositiveSampleCandidatesOfArticle(sampleCandidates)

      EventLogger raise Event(extractedSamplesFromPageRevisions,
        s"($number/$totalNumber) Extracted ${samples.size} samples from revs of $pageTitleInUri (Actor: ${self.path.name})",
        Json.obj("uriTitle" -> pageTitleInUri, "noOfSamples" -> samples.size, "noOfSampleCandidates" -> sampleCandidates.size))

      sender ! Status.Success
    } catch {
      case ex: WikiPageNotInCacheException =>
        EventLogger raise Event(wikipageDoesNoExist, s"($number/$totalNumber) Wikipage" +
          s" revisions not in cache: " + pageTitleInUri)
        sender ! Status.Success("no_wiki_data")
      case e: RevisionsOfResourceProcessingOutOfMemory =>
        log.error("freeing sample finder pipeline")
        sampleFinderPipeline = null
        Util.gc
        log.error("finished gc go on")
        sender ! Status.Success("error")
        throw e
      case e: Exception =>
        // send success to continue the extraction process, i.e. ensure files
        // get closed correctly in the end
        sender ! Status.Success("error")
        throw e
    }
  }

  private def extractDistinctSamples(pageTitleInUri: String): (Seq[Sample], Seq[NegativeSample], Seq[SampleCandidate]) = {

    val (samples, negativeSamples, sampleCandidates) = processText(pageTitleInUri) { rev =>

      val poNegSamplesAndSampleCandidates = sampleFinderPipeline
        .process(rev.content, rev.resourceURI, rev.wikipediaRevId, rev.wikipediaArticleName)

      if (log.isTraceEnabled) {
        log.trace(s"${rev.wikipediaArticleName}: ${rev.wikipediaRevId}:" + poNegSamplesAndSampleCandidates._1.size)
      }

      poNegSamplesAndSampleCandidates
    }
      .foldLeft((List.empty[Sample], List.empty[NegativeSample], List.empty[SampleCandidate])) { case (aggregate, sAndC) =>
      (aggregate._1 ++ sAndC._1, aggregate._2 ++ sAndC._2, aggregate._3 ++ sAndC._3)
    }


    val distinctSamples = samples.groupBy(s => s.sentenceText.replaceAll("\\s", "") + s.quad).map { case (_, equalSamples) =>
      val revisions = equalSamples.flatMap(_.revisionNumber)
      equalSamples.head.copy(revisionNumber = revisions)
    }.toSeq

    val distinctSampleCandidates = sampleCandidates
      .groupBy(_.sentenceText.replaceAll("\\s", ""))
      .map { case (_, equalSampleCandidates) =>

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

    // adapt size of negative samples
    val biasedSize = (distinctSamples.size + 1) * 5
    val distinctNegativeSamples = negativeSamples.groupBy(_.sentenceText).map { case (_, l) => l.head}
    val resizedNegativeSamples = (distinctNegativeSamples.size match {
      case i if i > biasedSize => scala.util.Random.shuffle(distinctNegativeSamples).take(biasedSize)
      case _ => distinctNegativeSamples
    }).toSeq

    log.info(s"$pageTitleInUri found ${distinctSamples.size} positive and ${resizedNegativeSamples.size} negative samples")

    (distinctSamples, resizedNegativeSamples, distinctSampleCandidates.toSeq)
  }
}