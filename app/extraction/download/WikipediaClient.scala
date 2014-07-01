package extraction.download

import java.net.{URLEncoder, URLDecoder, URL}
import scala.io.{Source, Codec}
import play.api.libs.json._
import models.{Event, Page, Revision}
import actors.events.EventLogger
import models.EventTypes._


object WikipediaClient {

  case class ResultParsingException(message: String) extends Exception(message)

  case class WikiPageDoesNotExistException(message: String) extends Exception(message)


  // use UTF8 for HTTP calls and IO
  implicit val codec = Codec.UTF8

  /** Returns a list of all known revisions for a certain Wikipedia article. */
  def enumerateRevisions(articleNameInUrl: String) = getRevisionStream(articleNameInUrl).toList


  /** Download a set of revisions for a certain Wikipedia article.
    *
    * Downloads a set of revisions belonging to the same Wikipedia article
    *
    * @param revisions a list of revisions which should be downloaded. Usually content property is None
    * @return a list of revisions which content property is filled, revisions with no content
    *         (e.g. deleted revisions) are filtered out
    */
  def downloadRevisionContents(revisions: Seq[Revision], wikipediaArticleName: String, dbPediaResourceName: String) = {
    val revisionsWithContent = fetchRevisionContents(revisions, wikipediaArticleName, dbPediaResourceName)
    filterDeletedRevisions(revisionsWithContent, wikipediaArticleName)
  }


  private def filterDeletedRevisions(revisionsWithContent: Seq[Option[Revision]], pageTitleInUri: String) = {
    val existingRevisions = revisionsWithContent.filter(_.isDefined).map(_.get)
    val i = revisionsWithContent.size - existingRevisions.size
    if (i > 0) {
      EventLogger raise Event(droppedRevisions, pageTitleInUri + " : Dropped " + i + " revisions without content")
    }
    existingRevisions
  }


  /** Custom stream to enumerate/list all revisions of a wiki article
    *
    * Listing revisions from the MediaWikiAPI has limits with respect to
    * the amount of data you receive in one request. If there is more data,
    * the result will have a continue element, see [[http://www.mediawiki.org/wiki/API:Query#Continuing_queries Continuing Queries]]
    *
    * This custom stream sents HTTP calls to receive further revisions lazily
    * when further revisions are requested (and available)
    *
    * @param articleNameInUrl
    * @return stream of revisions without content
    */
  private def getRevisionStream(articleNameInUrl: String): Stream[Revision] = {
    def continue(previousResult: JsValue): Stream[Revision] = {
      shouldContinue(previousResult) match {
        case Some(continueId) =>
          val continueResult = fetchUrlAsJson(buildContinueRevisionEnumerationURL(articleNameInUrl, continueId))
          buildRevisionsWithoutContentAndPageFrom(continueResult).toStream #::: continue(continueResult)
        case None => Stream.empty
      }
    }

    val firstResult = fetchUrlAsJson(buildInitialRevisionEnumerationURL(articleNameInUrl))
    checkWikiPageExists(firstResult, articleNameInUrl)
    buildRevisionsWithoutContentAndPageFrom(firstResult).toStream #::: continue(firstResult)
  }

  private def checkWikiPageExists(result: JsValue, articleNameInUrl: String): Unit = {
    (result \ "query" \ "pages" \ "-1").asOpt[JsObject] match {
      case Some(obj) => throw new WikiPageDoesNotExistException("Wikipage does not exist: " + articleNameInUrl)
      case _ => Unit
    }
  }


  private def fetchRevisionContents(revisions: Seq[Revision], wikipediaArticleName: String, dbPediaResourceName: String) = {
    // Media Wiki API allows max 50 revisions per request
    // => use 40 in order to have some buffer before we exhaust the API
    revisions.grouped(40).flatMap { revisionBatch =>
      val queryResult = fetchUrlAsJson(buildFetchRevisionsWithContentQueryUrl(revisionBatch))

      // Continue query not implemented yet -> We assume that a batch of 40 revisions
      // fits into one query; Refine implementation once needed
      assert(shouldContinue(queryResult) == None)

      val page = buildPage(queryResult, wikipediaArticleName, dbPediaResourceName)
      buildRevisionsWithContentAndPageFrom(queryResult, page)
    }.toSeq
  }

  private def shouldContinue(previousResult: JsValue): Option[Long] =
    (previousResult \ "continue" \ "rvcontinue").asOpt[Long]


  private def buildRevisionsWithContentAndPageFrom(queryResult: JsValue, page: Page) =
    selectRevisionsJsonArray(queryResult)
      .map(wikiJson => Revision.withContentFromWikiJsonAndPage(wikiJson, page))


  private def buildRevisionsWithoutContentAndPageFrom(queryResult: JsValue) =
    selectRevisionsJsonArray(queryResult)
      .map(wikiJson => Revision.withoutContentFromWikiJson(wikiJson))


  private def selectRevisionsJsonArray(queryResult: JsValue) =
    (queryResult \ "query" \\ "revisions")
      .map(revArray => revArray.as[List[JsObject]])
      .flatten

  private def buildPage(queryResult: JsValue, wikipediaArticleName: String, dbPediaResourceName: String) = {
    val id = (queryResult \ "query" \ "pages" \\ "pageid").headOption match {
      case Some(jsVal) => jsVal.asOpt[Long] match {
        case Some(long) => long
        case None => throw new ResultParsingException(wikipediaArticleName + ": Unable to parse pageId as Long")
      }
      case None => throw new ResultParsingException(wikipediaArticleName + ": Unable to find pageId")
    }

    val title = (queryResult \ "query" \ "pages" \\ "title").headOption match {
      case Some(jsVal) => jsVal.asOpt[String] match {
        case Some(str) => str
        case None => throw new ResultParsingException(wikipediaArticleName + ": Unable to parse page title as String")
      }
      case None => throw new ResultParsingException(wikipediaArticleName + ": Unable to find page title")
    }
    val decodedDbPediaResourceName = actors.Util.decodeResourceName(dbPediaResourceName)
    Page(id, title, dbPediaResourceName, decodedDbPediaResourceName, wikipediaArticleName)
  }

  case class UnableToFetchURLAsJSONException(message: String, t: Throwable) extends Exception(message, t)
  private def fetchUrlAsJson(url: URL) = try {
    val result = Source.fromURL(url)
    val jsString = result.getLines.mkString
    Json.parse(jsString)
  } catch {
    case ex: com.fasterxml.jackson.core.JsonParseException =>
      throw UnableToFetchURLAsJSONException(s"Fetching $url did not return valid JSON", ex)
    case ex: com.fasterxml.jackson.databind.JsonMappingException =>
      throw UnableToFetchURLAsJSONException(s"Fetching $url did not return mapable JSON", ex)
  }

  private def buildFetchRevisionsWithContentQueryUrl(revs: Seq[Revision]) = {
    val query = s"action=query&prop=revisions&revids=${revs.map(_.id).mkString("|")}&continue=&rvprop=timestamp|ids|content&format=json"
    buildWikiURLwithQuery(query)
  }

  private def buildContinueRevisionEnumerationURL(pageName: String, continueId: Long) = {
    val encPageName = URLEncoder.encode(pageName, "UTF-8")
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
    //val encPageName = pageName.replace("&", "%26")
    val encPageName = URLEncoder.encode(pageName, "UTF-8")
    val query = s"action=query&prop=revisions&titles=${encPageName}&rvlimit=max&continue=&rvend=2000-01-01T00:00:00Z&rvprop=timestamp|ids&format=json"
    buildWikiURLwithQuery(query)
  }

}