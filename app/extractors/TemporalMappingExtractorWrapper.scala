package extractors

//

import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.wikiparser.WikiParser
import org.dbpedia.extraction.mappings.{PageContext, MappingExtractor}
import ch.weisenburger.dbpedia.extraction.mappings.{RuntimeAnalyzer, TemporalQuad}
import models.{Quad, Revision}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.WikiPage
import play.api.Logger


case class NoContentException(message: String) extends Exception(message)

case class NoPageException(message: String) extends Exception(message)

// All dependencies we don't know for sure whether they are thread safe or not
class ThreadUnsafeDependencies {

  val wikiParser = WikiParser.getInstance()

  val englishTemporalMappingExtractor = new MappingExtractor(DBpediaExtractorFrameworkContextFactory.createContext(Language.English))

}

object ThreadUnsafeDependencies {
  def create(callerIdentifier: String) = synchronized {
    //Logger.info("Creating threadunsafe dependencies for " + callerIdentifier)
    new ThreadUnsafeDependencies
  }
}

class TemporalDBPediaMappingExtractorWrapper(threadUnsafeDependencies: ThreadUnsafeDependencies) {

  def extract(rev: Revision): Seq[Quad] = {
    checkPage(rev)
    checkContent(rev)

    val dbPediaExtractionFrameworkQuads = extractWithTemporalMappingExtractor(rev)

    dbPediaExtractionFrameworkQuads.map(convertDBPediaExtractorQuadsToCustomQuad(rev, _))
  }

  private def checkPage(revision: Revision) =
    if (revision.page.isEmpty)
      throw NoPageException(s"Revision ${revision.id} has no page information. Can't extract!")


  private def checkContent(revision: Revision) =
    if (revision.content.isEmpty)
      throw NoContentException(s"Revision ${revision.id} of page ${revision.page.get.uriTitle} has no content. Can't extract!")


  private def extractWithTemporalMappingExtractor(rev: Revision): Seq[org.dbpedia.extraction.destinations.Quad] = {
    assert(rev.wikiLanguage == Language.English)
    val analyzer = RuntimeAnalyzer(rev.subjectURI)

    analyzer.startWikiParser
    val page = buildWikiPage(rev.wikiTitle, rev.wikiLanguage, rev.content.get)
    val parsedPage = parseWikiPage(page)
    analyzer.stopWikiParser

    threadUnsafeDependencies.
      englishTemporalMappingExtractor.extract(parsedPage, rev.subjectURI, pageContext)
  }

  private def convertDBPediaExtractorQuadsToCustomQuad(rev: Revision, quad: org.dbpedia.extraction.destinations.Quad) = {
    quad match {
      case dbPediaQuad: TemporalQuad =>
        val fromDate  = dbPediaQuad.fromDate match {
          case Some(fromDate) => List("fromDate" -> fromDate)
          case None => List.empty
        }
        val toDate  = dbPediaQuad.toDate match {
          case Some(toDate) => List("toDate" -> toDate)
          case None => List.empty
        }

        Quad(dbPediaQuad.subject, dbPediaQuad.predicate, dbPediaQuad.value,
          (List("sourceRevision" -> rev.id.toString) ++ fromDate ++ toDate).toMap)
      case dbPediaQuad: org.dbpedia.extraction.destinations.Quad =>
        Quad(dbPediaQuad.subject, dbPediaQuad.predicate, dbPediaQuad.value, Map("sourceRevision" -> rev.id.toString))
    }
  }

  private def buildWikiPage(title: WikiTitle, lang: Language, revContent: String) = {
    new WikiPage(title, revContent)
  }

  private def parseWikiPage(page: WikiPage) = {
    threadUnsafeDependencies.wikiParser(page)
  }


  private def pageContext = new PageContext()

}