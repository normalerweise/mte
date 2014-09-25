package actors

import models.{Revision, TextRevision}
import org.slf4j.Logger
import scala.concurrent.Await
import scala.concurrent.duration._

/** Filter articles which length is above the 99 percentile
 *
 * avoids crashes of the pos-tagger due to nonsense input
 */
trait _99PercentileFilter {

  def isExcluded(contentLength: Int) = contentLength match {
    case i if i > 35000 => true
    case i if i < 100 => true
    case _ => false
  }

}

trait RevisionsOfResourceProcessor extends _99PercentileFilter {
  import Util._

  val log: Logger

  def processText[R](resourceNameInURI: String)(f: RuntimeRevision => R) = {
    process(resourceNameInURI, processText = true, excludeIfLengthHigherThan99Percentile = true)(f)
  }

  def processMarkup[R](resourceNameInURI: String)(f: RuntimeRevision => R) = {
    process(resourceNameInURI, processText = false, excludeIfLengthHigherThan99Percentile = true)(f)
  }

  private def process[R](resourceNameInURI: String, processText: Boolean, excludeIfLengthHigherThan99Percentile: Boolean)(f: RuntimeRevision => R) = {
    val revisionFuture = processText match {
      case true => TextRevision.getPageRevsWithPreprocessing(resourceNameInURI)
      case false => Revision.getPageRevs(resourceNameInURI)
    }

    val revisions = Await.result(revisionFuture, 60 seconds)

    for {
      revision <- revisions
      rev = toRuntimeRevision(revision)
      if checkExclusion(rev)
    } yield try {
      f(rev)
    } catch {
      case e: java.lang.OutOfMemoryError =>
        throw RevisionsOfResourceProcessingOutOfMemory(
          s"Out of memory exception processing ${rev.resourceNameInURI} rev ${rev.wikipediaRevId} (${buildWikipediaURL(rev.wikipediaArticleName, rev.wikipediaRevId)}); Input length ${rev.contentLength}", e)
    }
  }


  private def checkExclusion(rev: RuntimeRevision) =
    isExcluded(rev.contentLength) match {
    case true =>
      log.info(s"Skipped due to length restriction: ${rev.resourceNameInURI}; ${rev.contentLength}; ${buildWikipediaURL(rev.wikipediaArticleName, rev.wikipediaRevId)}")
      false
    case false =>
      log.trace(s"Processing ${rev.resourceNameInURI}  ${rev.wikipediaRevId}")
      true
  }

  private def toRuntimeRevision(revision: Revision) = {
    val resourceNameInURI = revision.page.get.dbpediaResourceName
    val resourceURI =  s"http://dbpedia.org/resource/${resourceNameInURI}"
    val wikipediaRevId = revision.id
    val wikipediaArticleName = revision.page.get.wikipediaArticleName
    val content = revision.content.getOrElse("")
    val contentLength = content.length
    RuntimeRevision(resourceNameInURI, resourceURI, wikipediaArticleName, wikipediaRevId, content, contentLength)
  }



}
case class RuntimeRevision(resourceNameInURI: String, resourceURI: String, wikipediaArticleName: String, wikipediaRevId: Long, content: String, contentLength: Int)
case class RevisionsOfResourceProcessingOutOfMemory(msg: String, t: Throwable = null) extends Exception(msg, t)