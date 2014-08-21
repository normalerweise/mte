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

    case SaveExtractedFactsOfArticle(facts, sender) => try {
      log.info(s"received ${facts.size} facts")
      saveExtractedFacts(facts)
      sender ! Status.Success("saved")
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
    relation = fact.quad.relation
    sRelation = fact.sRelation
    srBegin = sRelation.begin
    srEnd = sRelation.end
    rValue = sentenceText.substring(srBegin, srEnd)
  } {

    factFileWriter.append(
      s"""
          |${fact.articleName}: ${fact.revisionNumber}
          |Sentence:
          | ${sentenceText.replace("\n", " ")}
          |  P:$relation ($rValue, $srBegin, $srEnd)
        """.stripMargin)

    factFileWriter.flush
  }

}

case class SaveExtractedFactsOfArticle(facts: Seq[ExtractedFact], sender: ActorRef)