package extractors

import play.api.libs.concurrent.Execution.Implicits._
//import scala.concurrent.ExecutionContext.Implicits.global
import java.net.URL
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.{Source, Codec}
import play.api.libs.json._
import models.{Event, Page, Revision}
import play.api.libs.ws.WS
import actors.events.EventLogger
import models.EventTypes._

case class QueryResultParserException(message: String) extends Exception(message)
case class WikiPageDoesNotExistException(message: String) extends Exception(message)

object RelevantRevisionDownloader {
  // use UTF8 for HTTP calls and IO
  implicit val codec = Codec.UTF8


  def download(pageTitleInUri: String) = {
    val revisions = getRevisionStream(pageTitleInUri).toList
    val selectedRevisions = selectExtractionRelevantRevisions(revisions)
    val selectedRevisionsWithContent = fetchRevisionContents(selectedRevisions, pageTitleInUri)
    filterDeletedRevisions(selectedRevisionsWithContent, pageTitleInUri)
  }


  ////////////////////////////// 

  private def selectExtractionRelevantRevisions(revisions: List[Revision]): List[Revision] = {
    val revsByYear = revisions.groupBy(_.timestamp.year)
      .toList
      .sortBy {
      case (year, _) => year.get
    }

    val selectedRevs = revsByYear.map {
      case (_, revs) => selectRevisionsAtQuantiles(revs)
    }.flatten

    selectedRevs
  }

  private def selectRevisionsAtQuantiles(revs: List[Revision]): List[Revision] = {
    val length = revs.length
    if (length > 4) {
      revs.sortBy(_.timestamp.getMillis())
      val median = calcMedianIndex(length)
      val firstQuartile = calcFirstQuartil(length)
      val thirdQuartile = 3 * firstQuartile
      List(revs(firstQuartile), revs(median), revs(thirdQuartile), revs(length - 1))
    } else {
      revs
    }
  }

  private def filterDeletedRevisions(revisionsWithContent: Seq[Option[Revision]], pageTitleInUri: String) = {
    val existingRevisions = revisionsWithContent.filter(_.isDefined).map(_.get)
    val i =revisionsWithContent.size - existingRevisions.size
    if(i > 0) {
      EventLogger raise Event(droppedRevisions, pageTitleInUri +  " : Dropped " + i + " revisions without content")
    }
    existingRevisions
  }

  private def calcFirstQuartil(n: Int) = (n + 1) / 4

  private def calcMedianIndex(n: Int) = n match {
    // length is even or odd?
    case n: Int if n % 2 == 0 => (n + 1) / 2
    case n: Int => n / 2 // skip ((js)/2 + 1) => revs are discrete
  }

  private def getRevisionStream(pageName: String): Stream[Revision] = {
    def continue(previousResult: JsValue): Stream[Revision] = {
      shouldContinue(previousResult) match {
        case Some(continueId) =>
          val continueResult = fetchUrl(buildContinueRevisionEnumerationURL(pageName, continueId))
          getRevisions(continueResult).toStream #::: continue(continueResult)
        case None => Stream.empty
      }
    }
    val firstResult = fetchUrl(buildInitialRevisionEnumerationURL(pageName))
    checkWikiPageExists(firstResult, pageName)
    getRevisions(firstResult).toStream #::: continue(firstResult)
  }

  private def checkWikiPageExists(result: JsValue, pageTitleInUri: String) = {
    (result \ "query" \ "pages" \ "-1").asOpt[JsObject] match {
      case Some(obj) => throw new WikiPageDoesNotExistException("Wikipage does not exist: " + pageTitleInUri )
      case _ => Unit
    }
  }

  private def fetchRevisionContents(revs: List[Revision], pageTitleInUri: String) = {
    val queryResult = fetchUrl(buildFetchRevisionsWithContentQueryUrl(revs))
    // Continue query not implemented -> We assume that all revs for one page
    // fit into one query; Improve implementation once needed
    assert(shouldContinue(queryResult) == None)
    val page = getPage(queryResult, pageTitleInUri)
    getRevisionsWithContentAndPage(queryResult, page)
  }

  private def shouldContinue(previousResult: JsValue): Option[Long] = 
    (previousResult \ "continue" \ "rvcontinue").asOpt[Long]
  

  private def getRevisionsWithContentAndPage(queryResult: JsValue, page: Page) =
     getRevisionsAsJson(queryResult)
      .map(wikiJson => Revision.withContentFromWikiJsonAndPage(wikiJson, page))
  

  private def getRevisions(queryResult: JsValue) =
    getRevisionsAsJson(queryResult)
      .map(wikiJson => Revision.withoutContentFromWikiJson(wikiJson))


  private def getRevisionsAsJson(queryResult: JsValue) =
    (queryResult \ "query" \\ "revisions")
      .map(revArray => revArray.as[List[JsObject]])
      .flatten
  
  private def getPage(queryResult: JsValue, pageTitleInUri: String) = {
    val id = (queryResult \ "query" \ "pages" \\ "pageid").headOption match {
      case Some(jsVal) => jsVal.asOpt[Long] match {
        case Some(long) => long
        case None => throw new QueryResultParserException(pageTitleInUri + ": Unable to parse pageId as Long")
      }
      case None => throw new QueryResultParserException(pageTitleInUri + ": Unable to find pageId")
    }

    val title = (queryResult \ "query" \ "pages" \\ "title").headOption match {
      case Some(jsVal) => jsVal.asOpt[String] match {
        case Some(str) => str
        case None => throw new QueryResultParserException(pageTitleInUri + ": Unable to parse page title as String")
      }
      case None => throw new QueryResultParserException(pageTitleInUri + ": Unable to find page title")
    }

    Page(id, title, pageTitleInUri)
  }


  private def fetchUrl(url: URL) = {
    val result = Source.fromURL(url)
    Json.parse(result.getLines.mkString)
   // val result = Await.result(WS.url(url.toURI.toString).get.map( response => response.json ), 2 seconds)
  }

  private def buildFetchRevisionsWithContentQueryUrl(revs: List[Revision]) = {
    val query = s"action=query&prop=revisions&revids=${revs.map(_.id).mkString("|")}&continue=&rvprop=timestamp|ids|content&format=json"
    buildWikiURLwithQuery(query)
  }

  private def buildContinueRevisionEnumerationURL(pageName: String, continueId: Long) = {
    // TODO: strange....
    val encPageName = pageName.replace("&", "%26")
    val query = s"action=query&prop=revisions&titles=${encPageName}&rvlimit=max&continue=&rvcontinue=${continueId}&rvend=2000-01-01T00:00:00Z&rvprop=timestamp|ids&format=json"
    buildWikiURLwithQuery(query)
  }

  private def buildWikiURLwithQuery(query: String) = {
    val scheme = "http"
    val authority = "en.wikipedia.org"
    val path = "/w/api.php"
    val url = new URL(scheme + "://" + authority + path + "?" + query)
    url
  }

  private def buildInitialRevisionEnumerationURL(pageName: String) = {
    val encPageName = pageName.replace("&", "%26")
    val query = s"action=query&prop=revisions&titles=${encPageName}&rvlimit=max&continue=&rvend=2000-01-01T00:00:00Z&rvprop=timestamp|ids&format=json"
    buildWikiURLwithQuery(query)
  }

  //  def savePagesFromRevisionContensResult(fileName: String, result: xml.Elem) = {
  //    // a MediaWiki query result contains one pages element
  //    val pagesNode = (result \\ "pages")(0)
  //    XML.save(SavePath + File.separator + fileName + ".xml", pagesNode, codec.name)
  //  }

  //    def printSelectedRevsByYear(revisions: List[Revision]) = {
  //      val revsByYear = revisions.groupBy(_.timestamp.year)
  //        .toList.sortBy { case (year, _) => year.get }
  //
  //      val selectedRevs = revsByYear.map {
  //        case (_, revs) => selectRevisionsAtQuantiles(revs)
  //      }.flatten
  //
  //      for ((year, revs) <- revsByYear) yield {
  //        println(year.get + ": no of revisions " + revs.length);
  //        println("\tfirst:" + revs.head)
  //        println("\tlast:" + revs.last)
  //        println("\t4-quantiles by " + selectRevisionsAtQuantiles(revs).mkString(","))
  //      }
  //    }

}