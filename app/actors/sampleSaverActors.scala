package actors

import java.io.{FileWriter, BufferedWriter, Writer, File}

import actors.SampleFiles.Format
import akka.actor.{Actor, Status}
import ch.weisenburger.nlp.stanford.util.MaxEntClassifierFeatureFactory
import ch.weisenburger.uima.types.distantsupervision.skala._
import org.slf4j.LoggerFactory


class SampleSaverActor extends Actor {

  private var log = LoggerFactory.getLogger(getClass)

  private var extractionRunId: String = _
  private var sampleFiles: SampleFiles = _


  def receive = {

    case OpenExtractionRun(extractionRunId) =>
      openExtractionRunId(extractionRunId)
      sender ! Status.Success("opened")

    case CloseExtractionRun(extractionRunId) =>
      closeExtractionRunId(extractionRunId)
      sender ! Status.Success("closed")

    case SavePositiveSamplesOfArticle(samples) =>
      saveSamples(samples)

    case SaveNegativeSamplesOfArticle(negativeSamples) =>
      saveNegativeSamples(negativeSamples)
  }


  private def openExtractionRunId(extractionRunId: String) = {
    this.extractionRunId = extractionRunId
    this.sampleFiles = new SampleFiles(s"data/samples/$extractionRunId/")
    log.info(s"Opened files for runId $extractionRunId")
  }

  private def closeExtractionRunId(extractionRunId: String) = {
    // we don't support multiple extraction runs at the same time
    assert(extractionRunId == this.extractionRunId)
    sampleFiles.close
    log.info(s"Closed files for runId $extractionRunId")
  }


  private def saveSamples(samples: Seq[Sample]) = for {
    sample <- samples
  } {
    saveHumanReadableRepresentation(sample)
    saveStanfordCRFTrainRepresentation(sample)
    saveStanfordMaxEntTrainRepresentation(sample)
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
       |${sentenceText}
       |    Quad: <$qEntity, $qRelation, $qValue, $qTimex>
       |           $es, $rs, $vs, $ts
      """.stripMargin


    sampleFiles.positive(qRelation, Format.Human, sTimex.isDefined)
      .append(textRepresentation)
  }

  private def saveStanfordCRFTrainRepresentation(sample: Sample) = {
    val hasTimex = sample.sTimex.isDefined
    val relationURI = sample.quad.relation

    val textRepresentation = toStanfordCRFTRepresentation(sample.tokens)

    sampleFiles.positive(relationURI, Format.CRF, hasTimex)
      .append(textRepresentation)
  }

  private def saveStanfordMaxEntTrainRepresentation(sample: Sample) = {
    val relationURI = sample.quad.relation
    val hasTimex = sample.sTimex.isDefined

    val goldAnswer = models.Util.getLastUriComponent(relationURI)
    val textRepresentation = toStanfordMaxEntRepresentation(goldAnswer, sample.tokens, sample.sValue)

    sampleFiles.positive(relationURI, Format.MaxEnt, hasTimex)
      .append(textRepresentation)
  }


  private def saveNegativeSamples(negativeSamples: Seq[NegativeSample]) = for {
    negativeSample <- negativeSamples
  } {
    saveHumanReadableRepresentation(negativeSample)
    saveStanfordCRFTrainRepresentation(negativeSample)
    saveStanfordMaxEntTrainRepresentation(negativeSample)
  }

  private def saveHumanReadableRepresentation(negativeSample: NegativeSample) = {
    val revs = negativeSample.revisionNumber.mkString(",")
    val sentenceText = negativeSample.sentenceText
    val formattedNumbers = negativeSample.formattedNumbers.map { n =>
      s"${sentenceText.substring(n.begin, n.end)}; ${n.parsedNumericValue} (${n.begin}/${n.end})"
    }.mkString("\n  ")

    val textRepresentation =
      s"""
       |${negativeSample.articleName}: $revs
       |Sentence:
       |${sentenceText}
       |Numbers:
       |  $formattedNumbers
      """.stripMargin

    sampleFiles.negative.human.append(textRepresentation)
  }

  private def saveStanfordCRFTrainRepresentation(negativeSample: NegativeSample) = {
    val textRepresentation = toStanfordCRFTRepresentation(negativeSample.tokens)
    sampleFiles.negative.crf.append(textRepresentation)
  }

  private def saveStanfordMaxEntTrainRepresentation(negativeSample: NegativeSample) = {
    for (value <- negativeSample.formattedNumbers) {
      val textRepresentation = toStanfordMaxEntRepresentation("O", negativeSample.tokens, value)
      sampleFiles.negative.maxEnt.append(textRepresentation)
    }
  }

  private def toStanfordCRFTRepresentation(tokens: Seq[Token]) = {
    tokens
      .map(t => Seq(t.relationValueType.getOrElse("O"), t.text, t.lemma, t.posTag, t.namedEntityType.getOrElse("O")))
      .map(seq => seq.mkString("\t")).mkString("\n") + "\n\n"  // add newline after end of sample
  }

  private def toStanfordMaxEntRepresentation (goldAnswer: String, tokens: Seq[Token], value: Value) = {
    val features = MaxEntClassifierFeatureFactory.createFeatures(tokens, value)
    (Seq (goldAnswer) ++ features).mkString ("\t") + "\n"
  }

}


class SampleCandidateSaverActor extends Actor {

  import ch.weisenburger.deprecated_ner.FileUtil

  var log = LoggerFactory.getLogger(getClass)

  var sampleCandidatesFileWriter: Writer = _

  def receive = {

    case OpenExtractionRun(extractionRunId) =>
      openExtractionRunId(extractionRunId)
      sender ! Status.Success

    case CloseExtractionRun(extractionRunId) =>
      closeExtractionRunId(extractionRunId)
      sender ! Status.Success

    case SavePositiveSampleCandidatesOfArticle(sampleCandidates) =>
      log.info(s"received ${sampleCandidates.size}")
      saveSampleCandidates(sampleCandidates)
  }

  private def openExtractionRunId(extractionRunId: String) = {

    val sampleCandidatesFile = FileUtil.ensureExists(s"data/samples/$extractionRunId/sample_candidates.txt")
    sampleCandidatesFileWriter = new BufferedWriter(new FileWriter(sampleCandidatesFile))
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
          | ${sentenceText}
          |  S:$strEntities
          |  P:$strRelations
          |  O:$strValues
          |  T:$strTimexes
        """.stripMargin)

  }
}



object SampleFiles {

  object Format extends Enumeration {
    type Format = Value
    val Human, CRF, MaxEnt = Value
  }

}

class SampleFiles(runDirPath: String) {

  import SampleFiles.Format._
  import ch.weisenburger.deprecated_ner.FileUtil

  val posDirPath = runDirPath + "positive_samples/"
  val negDirPath = runDirPath + "negative_samples/"

  // ensure we don't have an old file structure present
  FileUtil.deleteFolder(new File(posDirPath))
  FileUtil.deleteFolder(new File(negDirPath))

  private lazy val negSamplesWriters = NegativeSampleFileWriters(
    newWriter(negDirPath + "human.txt"),
    newWriter(negDirPath + "crf.tsv"),
    newWriter(negDirPath + "maxent.tsv")
  )

  private val posSamplesWriters: collection.mutable.Map[String, PositiveSampleFileWritersOfRelation] = collection.mutable.HashMap.empty

  def negative = negSamplesWriters

  def positive(relationUri: String, format: Format, hasTimex: Boolean): Writer = {
    val writers = positive(relationUri)
    (hasTimex, format) match {
      case (true, Human) => writers.withTimexHuman
      case (false, Human) => writers.withoutTimexHuman
      case (true, CRF) => writers.withTimexCRF
      case (false, CRF) => writers.withoutTimexCRF
      case (true, MaxEnt) => writers.withTimexMaxEnt
      case (false, MaxEnt) => writers.withoutTimexMaxEnt
      case _ => throw new IllegalArgumentException(
        s"Invalid hasTimex, file format combination: $hasTimex; $format ")
    }
  }

  def positive(relationURI: String) =
    posSamplesWriters.getOrElse(relationURI, {
      val relationFolderName = models.Util.getLastUriComponent(relationURI)
      val writers = openForRelation(relationFolderName)
      posSamplesWriters(relationURI) = writers
      writers
    })

  private def openForRelation(relationFolderName: String) = {
    PositiveSampleFileWritersOfRelation(
      newWriter(posDirPath + s"$relationFolderName/withTimex.txt"),
      newWriter(posDirPath + s"$relationFolderName/withoutTimex.txt"),
      newWriter(posDirPath + s"$relationFolderName/withTimexCRF.tsv"),
      newWriter(posDirPath + s"$relationFolderName/withoutTimexCRF.tsv"),
      newWriter(posDirPath + s"$relationFolderName/withTimexMaxEnt.tsv"),
      newWriter(posDirPath + s"$relationFolderName/withoutTimexMaxEnt.tsv")
    )
  }

  private def newWriter(filePath: String) = {
    val file = FileUtil.ensureExists(filePath)
    new AlwaysFlushWriter(new BufferedWriter(new FileWriter(file)))
  }

  def close = {
    posSamplesWriters.foreach { case (_, w) => w.close}
    posSamplesWriters.clear
    negSamplesWriters.close
  }
}

class AlwaysFlushWriter(writer: Writer) extends Writer {
  override def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
    writer.write(cbuf, off, len)
    writer.flush
  }

  override def flush(): Unit = writer.flush

  override def close(): Unit = writer.close
}

case class PositiveSampleFileWritersOfRelation(withTimexHuman: Writer, withoutTimexHuman: Writer, withTimexCRF: Writer, withoutTimexCRF: Writer, withTimexMaxEnt: Writer, withoutTimexMaxEnt: Writer) {
  def close = {
    withTimexHuman.close
    withoutTimexHuman.close
    withTimexCRF.close
    withoutTimexCRF.close
    withTimexMaxEnt.close
    withoutTimexMaxEnt.close
  }
}

case class NegativeSampleFileWriters(human: Writer, crf: Writer, maxEnt: Writer) {
  def close = {
    human.close
    crf.close
    maxEnt.close
  }
}


case class OpenExtractionRun(extractionRunId: String)

case class CloseExtractionRun(extractionRunId: String)

case class SavePositiveSamplesOfArticle(samples: Seq[Sample])

case class SaveNegativeSamplesOfArticle(negativeSamples: Seq[NegativeSample])

case class SavePositiveSampleCandidatesOfArticle(sampleCandidates: Seq[SampleCandidate])

