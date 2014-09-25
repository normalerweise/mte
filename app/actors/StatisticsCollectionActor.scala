package actors

import actors.events.EventLogger
import akka.actor.{Status, Actor}
import ch.weisenburger.uima.FinancialDataPipelineFactory
import ch.weisenburger.uima.types.distantsupervision.skala.{SentenceStatistic, ExtractedFact}
import models.{TextRevision, Event}
import models.EventTypes._
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import scala.concurrent.duration._


import scala.concurrent.Await


case class CollectStatistics(extractionRunId: Option[BSONObjectID], number: Int, totalNumber: Int, pageTitleInUri: String)


class StatisticsCollectionActor extends Actor {

  val log = LoggerFactory.getLogger(getClass)

  val statisticsPipeline = FinancialDataPipelineFactory.createSentenceStatisticsPipeline
  val statisticsSaver = DefaultActors.statisticsSaver


  def receive = {
    case CollectStatistics(extractionRunId, number, totalNumber, pageTitleInUri) => try {
      implicit val _exid = extractionRunId
      val stats = collectStatistics(pageTitleInUri)
      statisticsSaver ! SaveExtractedStatsOfArticle(stats, sender)

      EventLogger raise Event(collectedStatisticsFromPageRevisions,
        s"($number/$totalNumber) Collected stats from revs of $pageTitleInUri (Actor: ${self.path.name})",
        Json.obj("uriTitle" -> pageTitleInUri))

    } catch {
      case ex: WikiPageNotInCacheException =>
        sender ! Status.Failure(ex)
        EventLogger raise Event(wikipageDoesNoExist, s"($number/$totalNumber) Wikipage revisions not in cache: " + pageTitleInUri)
      case e: Exception =>
        sender ! Status.Success("error")
        throw e
    }
  }

  private def collectStatistics(pageTitleInUri: String): Seq[SentenceStatistic] = {
    log.info("processing " + pageTitleInUri)
    val revisions = Await.result(TextRevision.getPageRevs(pageTitleInUri), 30 seconds)

    val allStats = revisions.flatMap { rev =>
      val wikiText = rev.content.get
      val dbpediaResourceName = rev.page.get.dbpediaResourceName
      val dbpediaResourceURI = s"http://dbpedia.org/resource/${dbpediaResourceName}"
      val wikiRevId = rev.id
      val wikiArticleName = rev.page.get.wikipediaArticleName

        val stats = statisticsPipeline
          .process(wikiText, dbpediaResourceURI, wikiRevId, wikiArticleName)


        stats
    }
    allStats
  }



}
