package actors

import java.io.{FileWriter, BufferedWriter, Writer}

import akka.actor.{ActorRef, Status, Actor}
import ch.weisenburger.uima.types.distantsupervision.skala.{SampleCandidate, ExtractedFact, Sample}
import controllers.FileUtil
import org.slf4j.LoggerFactory

/**
 * Created by Norman on 20.08.14.
 */
class FactSaverActor extends Actor {

  var log = LoggerFactory.getLogger(getClass)
  var extractionRunId: String = _

  var factFileWriter: Writer = _

  def receive = {

    case OpenExtractionRun(extractionRunId) =>
      openExtractionRunId(extractionRunId)
      sender ! Status.Success("opened")

    case CloseExtractionRun(extractionRunId) =>
      closeExtractionRunId(extractionRunId)
      sender ! Status.Success("closed")

    case SaveExtractedFactsOfArticle(facts, originalSender) => try {
      saveExtractedFacts(facts)
      originalSender ! Status.Success("saved")
    }
  }

  private def openExtractionRunId(extractionRunId: String) = {

    val factFile = FileUtil.ensureExists(s"data/facts/$extractionRunId/facts.txt")
    factFileWriter = new BufferedWriter(new FileWriter(factFile))
    log.info(s"opened files for runId $extractionRunId")
  }

  private def closeExtractionRunId(extractionRunId: String) = {
    factFileWriter.close
    factFileWriter = null
    log.info(s"closed files for runId $extractionRunId")
  }

  private def saveExtractedFacts(facts: Seq[ExtractedFact]) = for {
    fact <- facts
    sentenceText = fact.sentenceText
    qEntity = fact.quad.entity
    qRelation = fact.quad.relation
    qValue = fact.quad.value
    qTimex = fact.quad.timex.getOrElse("?")

    sRelation = fact.sRelation
    srBegin = sRelation.begin
    srEnd = sRelation.end
    sValue = fact.sValue
    svBegin = sValue.begin
    svEnd = sValue.end
    sEntities = fact.sEntities
    sTimexes = fact.sTimexes
  } {

    val entityString = sEntities.map(e => s"${e.dbpediaResourceUri} (${e.begin},${e.end})").mkString(", ")
    val timexString = sTimexes.map(t => s"${t.value} (${t.begin},${t.end})").mkString(", ")

    factFileWriter.append(
      s"""
          |${fact.articleName}: ${fact.revisionNumber}
          |Sentence:
          | ${sentenceText}
          |Quad: <$qEntity, $qRelation, $qValue, $qTimex>
          |   S:$entityString
          |   P:${sRelation.dbpediaOntologyUri} ($srBegin, $srEnd)
          |   O:${sValue.parsedNumericValue} ($svBegin, $svEnd)
          |   T:$timexString
        """.stripMargin)

    factFileWriter.flush
  }

}

case class SaveExtractedFactsOfArticle(facts: Seq[ExtractedFact], sender: ActorRef)