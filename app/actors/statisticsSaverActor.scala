package actors

import java.io.{FileWriter, BufferedWriter, Writer}

import akka.actor.{ActorRef, Status, Actor}
import ch.weisenburger.uima.types.distantsupervision.skala.{SentenceStatistic, ExtractedFact}
import controllers.FileUtil
import org.slf4j.LoggerFactory

/**
 * Created by Norman on 04.09.14.
 */
class StatisticsSaverActor extends Actor {

  var log = LoggerFactory.getLogger(getClass)
  var extractionRunId: String = _

  var statsFileWriter: Writer = _

  def receive = {

    case OpenExtractionRun(extractionRunId) =>
      openExtractionRunId(extractionRunId)
      sender ! Status.Success("opened")

    case CloseExtractionRun(extractionRunId) =>
      closeExtractionRunId(extractionRunId)
      sender ! Status.Success("closed")

    case SaveExtractedStatsOfArticle(stats, originalSender) =>
      saveStats(stats)
      originalSender ! Status.Success("saved")
  }

  private def openExtractionRunId(extractionRunId: String) = {

    val factFile = FileUtil.ensureExists(s"data/stats/$extractionRunId/stats.tsv")
    statsFileWriter = new BufferedWriter(new FileWriter(factFile))
    log.info(s"opened files for runId $extractionRunId")
  }

  private def closeExtractionRunId(extractionRunId: String) = {
    statsFileWriter.close
    statsFileWriter = null
    log.info(s"closed files for runId $extractionRunId")
  }

  private def saveStats(stats: Seq[SentenceStatistic]) = for {
    stat <- stats
  } {
    statsFileWriter.append(
      s"${stat.articleName}\t${stat.revisionNumber}\t${stat.length}\n")
    statsFileWriter.flush
  }

}

case class SaveExtractedStatsOfArticle(stats: Seq[SentenceStatistic], sender: ActorRef)