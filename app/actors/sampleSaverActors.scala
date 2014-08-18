package actors

import java.io.{File, BufferedWriter, FileWriter, Writer}

import akka.actor.{Actor, ActorRef, Status}
import ch.weisenburger.uima.types.distantsupervision.skala._
import controllers.FileUtil
import org.slf4j.LoggerFactory

/**
 * Created by Norman on 14.07.14.
 */
class SampleSaverActor extends Actor {

  var log = LoggerFactory.getLogger(getClass)
  var extractionRunId: String = _

  val posSampleWriters: collection.mutable.Map[String, SampleFileWriters] = collection.mutable.HashMap.empty

  var negSamplesCRFTrainFileWriter: Writer = _


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
    case SaveNegativeSamplesOfArticle(negativeSamples, sender) => try {
      log.info(s"received ${negativeSamples.size} negative samples")
      saveNegativeSamples(negativeSamples)
      sender ! Status.Success("saved")

    } catch {
      case e: Exception => sender ! Status.Failure(e); throw e
    }
  }

  private def openExtractionRunId(extractionRunId: String) = {
    this.extractionRunId = extractionRunId

    ch.weisenburger.deprecated_ner.FileUtil.deleteFolder(new File(s"data/samples/$extractionRunId/"))

    val negSamplesCRFTrainFile = FileUtil.ensureExists(s"data/samples/$extractionRunId/negative_samples/negative_samples.tsv")
    negSamplesCRFTrainFileWriter = new BufferedWriter(new FileWriter(negSamplesCRFTrainFile))

    log.info(s"opened files for runId $extractionRunId")
  }

  private def closeExtractionRunId(extractionRunId: String) = {
    negSamplesCRFTrainFileWriter.close
    negSamplesCRFTrainFileWriter = null

    posSampleWriters.foreach { case (_, w) => w.close}
    posSampleWriters.clear

    log.info(s"closed files for runId $extractionRunId")
  }

  private def saveSamples(samples: Seq[Sample]) = for {
    sample <- samples
  } {
    saveHumanReadableRepresentation(sample)
    saveStanfordCRFTrainRepresentation(sample)
  }

  private def saveNegativeSamples(negativeSamples: Seq[NegativeSample]) = for {
    negativeSample <- negativeSamples
  } {
    saveStanfordCRFTrainRepresentation(negativeSample)
  }

  private def saveHumanReadableRepresentation(sample: Sample) = {
    val sentenceText = sample.sentenceText
    val sEntity = sample.sEntity
    val sRelation = sample.sRelation
    val sValue = sample.sValue
    val sTimex = sample.sTimex
    val quad = sample.quad
    val qEntity = quad.entity
    val qRelation = quad.relation
    val qValue = quad.value
    val qTimex = quad.timex
    val revs = sample.revisionNumber.mkString(", ")

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

    val textRepresentation =
      s"""
       |${sample.articleName}: $revs
       |Sentence:
       |${sentenceText.replace("\n", " ")}
       |    Quad: <$qEntity, $qRelation, $qValue, $qTimex>
       |           $es, $rs, $vs, $ts
      """.stripMargin

    val writer = getWriter(sTimex.isDefined, true, qRelation)
    writer.append(textRepresentation)
    writer.flush
  }

  private def saveStanfordCRFTrainRepresentation(negativeSample: NegativeSample) = {
    val textRepresentation = toStanfordCRFTRepresentation(negativeSample.tokens)
    negSamplesCRFTrainFileWriter.append(textRepresentation)
    negSamplesCRFTrainFileWriter.flush
  }

  private def saveStanfordCRFTrainRepresentation(sample: Sample) = {
    val sTimex = sample.sTimex
    val qRelation = sample.quad.relation

    val textRepresentation = toStanfordCRFTRepresentation(sample.tokens)

    val writer = getWriter(sTimex.isDefined, false, qRelation)
    writer.append(textRepresentation)
    writer.flush
  }

  private def toStanfordCRFTRepresentation(tokens: Seq[Token]) = tokens
    .map(t => Seq(t.relationValueType.getOrElse("O"), t.text, t.lemma, t.posTag, t.namedEntityType.getOrElse("O")))
    .map(seq => seq.mkString("\t")).mkString("\n") + "\n\n" // add newline after end of sample


  private def getWriter(hasTimex: Boolean, humanReadable: Boolean, relationUri: String) = {
    val relation = ch.weisenburger.deprecated_ner.Util.getLastUriComponent(relationUri)
    val writers = posSampleWriters.getOrElse(relation, {
      val path = s"data/samples/$extractionRunId/positive_samples/"
      val writers = SampleFileWriters.apply(path, relation)
      posSampleWriters(relation) = writers
      writers
    })

    (hasTimex, humanReadable) match {
      case (true, true) => writers.withTimexHuman
      case (false, false) => writers.withoutTimexStanfordCRFTrain
      case (true, false) => writers.withTimexStanfordCRFTrain
      case (false, true) => writers.withoutTimexHuman
    }

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
      s"${r.dbpediaOntologyUri} (${r.begin}, ${r.end})").mkString("; ")

    val strValues = values.map(v =>
      s"${v.parsedNumericValue} (${v.begin}, ${v.end})").mkString("; ")

    val strTimexes = timexes.map(t =>
      s"${t.value} (${t.begin}, ${t.end})").mkString("; ")

    val strEntities = entities.map(e =>
      s"${e.dbpediaResourceUri} (${e.begin}, ${e.end})").mkString("; ")

    val revs = sc.revisionNumber.mkString(", ")

    sampleCandidatesFileWriter.append(
      s"""
          |${sc.articleName}: $revs
          |Sentence:
          | ${sentenceText.replace("\n", " ")}
          |  S:$strEntities
          |  P:$strRelations
          |  O:$strValues
          |  T:$strTimexes
        """.stripMargin)

  }
}

object SampleFileWriters {
  def apply(dirPath: String, relation: String) = {
    new SampleFileWriters(
      newWriter(dirPath + s"$relation/withTimex.txt"),
      newWriter(dirPath + s"$relation/withoutTimex.txt"),
      newWriter(dirPath + s"$relation/withTimex.tsv"),
      newWriter(dirPath + s"$relation/withoutTimex.tsv")
    )
  }

  private def newWriter(filePath: String) = {
    val file = FileUtil.ensureExists(filePath)
    new BufferedWriter(new FileWriter(file))
  }

}

case class SampleFileWriters(withTimexHuman: Writer, withoutTimexHuman: Writer, withTimexStanfordCRFTrain: Writer, withoutTimexStanfordCRFTrain: Writer) {
  def close = {
    withTimexHuman.close
    withoutTimexHuman.close
    withTimexStanfordCRFTrain.close
    withoutTimexStanfordCRFTrain.close
  }
}

case class OpenExtractionRun(extractionRunId: String)

case class CloseExtractionRun(extractionRunId: String)

case class SaveSamplesOfArticle(samples: Seq[Sample], sender: ActorRef)

case class SaveNegativeSamplesOfArticle(negativeSamples: Seq[NegativeSample], sender: ActorRef)

case class SaveSampleCandidatesOfArticle(sampleCandidates: Seq[SampleCandidate], sender: ActorRef)

