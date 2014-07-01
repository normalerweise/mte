package actors

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.{ActorLogging, Actor}
import actors.events.EventLogger
import reactivemongo.bson.BSONObjectID
import monitoring.Instrumented
import play.api.libs.json.Json
import models.{Revision, Event}
import models.EventTypes._
import extraction.download.{WikipediaClient, WikipediaRevisionSelector}
import extraction.download.WikipediaClient.{UnableToFetchURLAsJSONException, WikiPageDoesNotExistException}
import java.net.URLDecoder


// Commands for the article downloader actor
case class DownloadAndSaveArticle(extractionRunId: Option[BSONObjectID], number: Int,
                                  totalNumber: Int, articleNameInUrl: String)


/** Select, Download and Store Wikipedia Revisions of articles
  *
  * Actor wraps the tasks of selecting interesting Wikipedia revisions
  *  (revisions with as much different temporal triples as possible),
  *  downloading those,
  *  and storing them in the revision cache
  *
  * Actor model serves parallelization and resilience against exceptions
  *
  */
class ArticleDownloaderActor extends Actor with ActorLogging with Instrumented {

  def receive = {
    case DownloadAndSaveArticle(extractionRunId, number, totalNumber, articleNameInUrl) => try {
      implicit val _extractionRunId = extractionRunId

      val das = metrics.timer("download_and_save").timerContext
      downloadAndSaveArticle(articleNameInUrl)
      val runtime = (das.stop nanoseconds).toMillis

      EventLogger raise Event(downloadedPageRevisions,
        s"($number/$totalNumber) Downloaded revs of $articleNameInUrl (Actor: ${self.path.name}, Runtime $runtime ms)",
          Json.obj("runtime" -> runtime, "uriTitle" -> articleNameInUrl))

    } catch {
      case ex: UnableToFetchURLAsJSONException =>
        EventLogger raise Event(unableToFetchRevisionInvalidJson,
          s"($number/$totalNumber) Unable to fetch revisions, invalid JSON: " + articleNameInUrl); None
      case ex: WikiPageDoesNotExistException =>
        EventLogger raise Event(wikipageDoesNoExist,
          s"($number/$totalNumber) Wikipage does not exist: " + articleNameInUrl); None
      case ex: AmbigousRedirectException =>
        EventLogger raise Event(ambiguousRedirect,
          s"($number/$totalNumber) $articleNameInUrl ${ex.getMessage}"); None
    }
  }

  private def downloadAndSaveArticle(dbPediaResourceName: String)(implicit extractionRunId: Option[BSONObjectID]) = try {

    val st = metrics.timer("store_revision_contents_to_mongo")

    val wikipediaArticleName = Util.decodeResourceName(dbPediaResourceName)

      //URLDecoder.decode(dbPediaResourceName, "UTF-8")

    val revisionsWithContent = {
      val revsWithContent = selectAndDownload(wikipediaArticleName, dbPediaResourceName).toSeq
       shouldRedirect(revsWithContent) match {
        case Some(targetArtcitleNameInUrl) => selectAndDownload(targetArtcitleNameInUrl, dbPediaResourceName)
        case None => revsWithContent
      }
    }

    val result = st.time {
      Await.result(Revision.deleteAllRevisionsOf(dbPediaResourceName), 5 seconds)
      Await.result(Revision.saveBulk(revisionsWithContent), 10 seconds)
    }
    result.map { lastError => lastError.ok match {
      case false => {
        val message = s"$dbPediaResourceName: Save failed: [lastError=$lastError]"
        log.error(message)
        EventLogger raise Event(exception, message)
      }
      case true => // noop
    }
    }

  }

  private val originalArticleName = (originalArticleNameInUrl: String,r: Revision) => {
   val oldPage = r.page.get
   r.copy(page = Some(oldPage.copy(dbpediaResourceName = originalArticleNameInUrl)))
  }




  /** Regular Expression to determine redirects in the content part of a revision query
   *
   * Examples which should result in a group match,
   *  which indicates article name the redirect points to:
   *  - "#REDIRECT [\[Rolls-Royce Holdings]\]\n{{R from move}}" -> Rolls-Royce Holdings
   *  - "#REDIRECT [\[Rolls-Royce Holdings]\]{{R from move}}" -> Rolls-Royce Holdings
   *  - "#REDIRECT [\[Rolls-Royce Holdings]\]" -> Rolls-Royce Holdings
   */
  private val redirectRegex = """(?i)#REDIRECT \[\[(.*)\]\]\s*.*""".r
  
  case class AmbigousRedirectException(message: String) extends Exception(message)
  def shouldRedirect(revs: Seq[Revision]) = {
    val redirectRevisions = revs.filter(_.content.get.toUpperCase.startsWith("#REDIRECT"))

    val percentOfRedirectRevisions = redirectRevisions.size.toDouble / (revs.size)

    // Decide on the amount of redirect revisions whether we use the current
    // or the redirect target page
    if(percentOfRedirectRevisions > 0.5) {
    val redirectTargets = redirectRevisions
     .flatMap { rev =>
      val m  = redirectRegex.findAllIn(rev.content.get)
      if(m.hasNext) Some(m.group(1), rev.id) else None
     }
     .filterNot( _._1.toLowerCase.contains("disambiguation") )
     .groupBy(_._1)
     .map(targetAndRevs =>
      (targetAndRevs._1, targetAndRevs._2.map(_._2).toList.sorted.last ))

     if(redirectTargets.size > 1) {
      //throw AmbigousRedirectException("Ambiguous redirects: " + redirectTargets.mkString(", "))
       val latestRedirect = redirectTargets.toList.sortBy(_._2).last
       Some(latestRedirect._1)
     } else if(redirectTargets.size == 1) {
       Some(redirectTargets.head._1)
     } else {
       None
     }
    }else{
      None
    }
  }

  def selectAndDownload(wikipediaArticleName: String, dbPediaResourceName: String) = {
    val en = metrics.timer("enumerate_and_select_revisions")
    val dl = metrics.timer("download_revision_contents")

    val revisions = en.time {
      WikipediaRevisionSelector.getRevisionsForExtraction(wikipediaArticleName,
        WikipediaRevisionSelector.revisionsAtQuartilesPlusLatestForEachYear)
    }

    dl.time {
      WikipediaClient.downloadRevisionContents(revisions, wikipediaArticleName, dbPediaResourceName)
    }

  }

}
