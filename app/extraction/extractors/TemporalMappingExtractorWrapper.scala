package extraction.extractors

import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.wikiparser.WikiParser
import org.dbpedia.extraction.mappings.{PageContext, MappingExtractor}
import ch.weisenburger.dbpedia.extraction.mappings.ExtendedQuad
import models.{Quad, Revision}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.WikiPage
import extraction.parser.AdvancedTimexParser


case class NoContentException(message: String) extends Exception(message)

case class NoPageException(message: String) extends Exception(message)

// All dependencies we don't know for sure whether they are thread safe or not
class Dependencies() {

  val wikiParser = WikiParser.getInstance()

  val advancedTimexParser = new AdvancedTimexParser

  val context = DBpediaExtractorFrameworkContextFactory.createContextWithTimexParser(Language.English, advancedTimexParser)

}


class TemporalDBPediaMappingExtractorWrapper(dependencies: Dependencies) {

  val englishTemporalMappingExtractor = new MappingExtractor(dependencies.context)

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
      throw NoContentException(s"Revision ${revision.id} of page ${revision.page.get.dbpediaResourceName} has no content. Can't extract!")


  private def extractWithTemporalMappingExtractor(rev: Revision): Seq[org.dbpedia.extraction.destinations.Quad] = {
    assert(rev.wikiLanguage == Language.English)
    val page = buildWikiPage(rev.wikiTitle, rev.wikiLanguage, rev.content.get)
    val parsedPage = parseWikiPage(page)

    englishTemporalMappingExtractor.extract(parsedPage, rev.subjectURI, pageContext)
  }

  private def convertDBPediaExtractorQuadsToCustomQuad(rev: Revision, quad: org.dbpedia.extraction.destinations.Quad) = {
    quad match {
      case dbPediaQuad: ExtendedQuad =>
        val fromDate  = dbPediaQuad.fromDate match {
          case Some(fromDate) => List("fromDate" -> fromDate)
          case None => List.empty
        }
        val toDate  = dbPediaQuad.toDate match {
          case Some(toDate) => List("toDate" -> toDate)
          case None => List.empty
        }

        Quad(dbPediaQuad.subject, dbPediaQuad.predicate, dbPediaQuad.value, Option(dbPediaQuad.datatype), Option(dbPediaQuad.language),
          (List("sourceRevision" -> rev.id.toString) ++ fromDate ++ toDate).toMap)
      case dbPediaQuad: org.dbpedia.extraction.destinations.Quad =>
        Quad(dbPediaQuad.subject, dbPediaQuad.predicate, dbPediaQuad.value, Option(dbPediaQuad.datatype), Option(dbPediaQuad.language), Map("sourceRevision" -> rev.id.toString))
    }
  }

  private def buildWikiPage(title: WikiTitle, lang: Language, revContent: String) = {
    new WikiPage(title, revContent)
  }

  private def parseWikiPage(page: WikiPage) = {
    dependencies.wikiParser(page)
    //WikiParser.getInstance().apply(page)
  }


  private def pageContext = new PageContext()

}