package actors

import java.io.{FileWriter, BufferedWriter, Writer, File}

import akka.actor.{Status, ActorRef, Actor}
import ch.weisenburger.uima.types.distantsupervision.skala._
import controllers.FileUtil
import org.slf4j.LoggerFactory
import reactivemongo.bson.BSONObjectID

/**
 * Created by Norman on 14.07.14.
 */
class SampleSaverActor extends Actor {

  var log = LoggerFactory.getLogger(getClass)
  var samplesFileWriter: Writer = _
  var samplesWithTimexFileWriter: Writer = _

  def receive = {
    case OpenExtractionRun(extractionRunId) =>
      openExtractionRunId(extractionRunId)
      sender ! Status.Success("opened")
    case CloseExtractionRun(extractionRunId) =>
      closeExtractionRunId(extractionRunId)
      sender ! Status.Success("closed")
    case SaveSamplesOfArticle(samples, sender) => try {
      log.info(s"received ${samples.size}")
      saveSamples(samples)
      sender ! Status.Success("saved")
    } catch {
      case e: Exception => sender ! Status.Failure(e); throw e
    }
  }

  private def openExtractionRunId(extractionRunId: String) = {
    val samplesFile = FileUtil.ensureExists(s"data/samples/$extractionRunId/matched_samples.txt")
    samplesFileWriter = new BufferedWriter(new FileWriter(samplesFile))

    val samplesWithTimexFile = FileUtil.ensureExists(s"data/samples/$extractionRunId/matched_samples_with_timex.txt")
    samplesWithTimexFileWriter = new BufferedWriter(new FileWriter(samplesWithTimexFile))

    log.info(s"opened files for runId $extractionRunId")
  }

  private def closeExtractionRunId(extractionRunId: String) = {
    samplesFileWriter.close
    samplesFileWriter = null

    samplesWithTimexFileWriter.close
    samplesWithTimexFileWriter = null
    log.info(s"closed files for runId $extractionRunId")
  }

  private def saveSamples(samples: Seq[Sample]) = for {
    sample <- samples
    sentenceText = sample.sentenceText
    sEntity = sample.sEntity
    sRelation = sample.sRelation
    sValue = sample.sValue
    sTimex = sample.sTimex
    quad = sample.quad
    qEntity = quad.entity
    qRelation = quad.relation
    qValue = quad.value
    qTimex = quad.timex
  } {

    val es = s"(${sEntity.begin}, ${sEntity.end})".padTo(qEntity.length, " ").mkString
    val rs = (sRelation match {
      case None => "?"
      case Some(r) => s"(${r.begin}, ${r.end})"
    }).padTo(qRelation.length, " ").mkString
    val vs = s"(${sValue.begin}, ${sValue.end})".padTo(qValue.length, " ").mkString
    val ts = (sTimex match {
      case None => "?"
      case Some(t) => s"(${t.begin}, ${t.end})"
    }).padTo(qTimex.getOrElse("?").length, " ").mkString

    val revs = sample.revisionNumber.mkString(", ")

    val textRepresentation =
      s"""
       |${sample.articleName}: $revs
       |Sentence:
       |${sentenceText.replace("\n"," ")}
       |    Quad: <$qEntity, $qRelation, $qValue, $qTimex>
       |           $es, $rs, $vs, $ts
      """.stripMargin

    if(sTimex.isDefined)
      samplesWithTimexFileWriter.append(textRepresentation)
    else
      samplesFileWriter.append(textRepresentation)

  }

}


class SampleCandidateSaverActor extends Actor {

  var log = LoggerFactory.getLogger(getClass)

  var sampleCandidatesFileWriter: Writer = _

  def receive = {
    case OpenExtractionRun(extractionRunId) =>
      openExtractionRunId(extractionRunId)
      sender ! Status.Success
    case CloseExtractionRun(extractionRunId) =>
      closeExtractionRunId(extractionRunId)
      sender ! Status.Success
    case SaveSampleCandidatesOfArticle(sampleCandidates, sender) => try {
      log.info(s"received ${sampleCandidates.size}")
      saveSampleCandidates(sampleCandidates)
      sender ! Status.Success
    } catch {
      case e: Exception => sender ! Status.Failure(e); throw e
    }
  }

  private def openExtractionRunId(extractionRunId: String) = {
    val sampleCandidatessFile = FileUtil.ensureExists(s"data/samples/$extractionRunId/sample_candidates.txt")
    sampleCandidatesFileWriter = new BufferedWriter(new FileWriter(sampleCandidatessFile))
    log.info(s"opened files for runId $extractionRunId")
  }

  private def closeExtractionRunId(extractionRunId: String) = {
    sampleCandidatesFileWriter.close
    sampleCandidatesFileWriter = null
    log.info(s"closed files for runId $extractionRunId")
  }

  private def saveSampleCandidates(sampleCandidates: Seq[SampleCandidate]) = for {
    sc <- sampleCandidates
    sentenceText = sc.sentenceText
    relations = sc.relations
    values = sc.values
    timexes = sc.timexes
    entities = sc.entities
  } {
    val strRelations = relations.map(r =>
      s"${r.dbpediaOntologyUri} (${r.begin}, ${r.end})" ).mkString("; ")

    val strValues = values.map(v =>
      s"${v.parsedNumericValue} (${v.begin}, ${v.end})").mkString("; ")

    val strTimexes = timexes.map( t =>
      s"${t.value} (${t.begin}, ${t.end})").mkString("; ")

    val strEntities = entities.map(e =>
      s"${e.dbpediaResourceUri} (${e.begin}, ${e.end})").mkString("; ")

    val revs = sc.revisionNumber.mkString(", ")

    sampleCandidatesFileWriter.append(
      s"""
          |${sc.articleName}: $revs
          |Sentence:
          | ${sentenceText.replace("\n"," ")}
          |  S:$strEntities
          |  P:$strRelations
          |  O:$strValues
          |  T:$strTimexes
        """.stripMargin)

  }

}

case class OpenExtractionRun(extractionRunId: String)
case class CloseExtractionRun(extractionRunId: String)
case class SaveSamplesOfArticle(samples: Seq[Sample], sender: ActorRef)
case class SaveSampleCandidatesOfArticle(sampleCandidates: Seq[SampleCandidate], sender: ActorRef)

