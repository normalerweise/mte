package extractors
//
//import org.dbpedia.extraction.util.Language
//import org.dbpedia.extraction.sources.WikiPage
//import org.dbpedia.extraction.wikiparser.WikiTitle
//import org.dbpedia.extraction.wikiparser.WikiParser
//import org.dbpedia.extraction.mappings.PageContext
//import ch.weisenburger.dbpedia.extraction.mappings.{RuntimeAnalyzer, TemporalQuad}
//
object TemporalMappingExtractorWrapper {
//
//  def extract(page: PageWithRevisionsContent) = {
//    val analyzer = RuntimeAnalyzer(buildSubjectURI(WikiTitle.parse(page.title, Language.English)))
//    analyzer.start
//    val quads = page.revisions.map(r => extractRevision(page.title, Language.English, r.content)).flatten
//    analyzer.stop
//    mapDBPediaExtractorQuadsToCustomQuads(quads)
//  }
//
//  def mapDBPediaExtractorQuadsToCustomQuads(quads: Seq[org.dbpedia.extraction.destinations.Quad]) = {
//    quads.map( _ match {
//      case dbPediaQuad: TemporalQuad => Quad(dbPediaQuad.subject, dbPediaQuad.predicate, dbPediaQuad.value, Map( "fromDate" -> dbPediaQuad.fromDate, "toDate" -> dbPediaQuad.toDate))
//      case dbPediaQuad: org.dbpedia.extraction.destinations.Quad => Quad(dbPediaQuad.subject, dbPediaQuad.predicate, dbPediaQuad.value, Map.empty[String,String])
//    })
//  }
//
//  def extractRevision(pageTitle: String, pageLang: Language, revContent: String): Seq[org.dbpedia.extraction.destinations.Quad] = {
//    val wikiTitle = WikiTitle.parse(pageTitle, pageLang)
//    val subjectUri = buildSubjectURI(wikiTitle)
//
//    val analyzer = RuntimeAnalyzer(subjectUri)
//
//    analyzer.startWikiParser
//    val page = buildWikiPage(wikiTitle, pageLang, revContent)
//    val parsedPage = parseWikiPage(page)
//    analyzer.stopWikiParser
//
//
//    DBPediaExtractorWrapper.extract(Language.English, parsedPage, subjectUri, pageContext)
//  }
//
//  def buildWikiPage(title: WikiTitle, lang: Language, revContent: String) = {
//    new WikiPage(title, revContent)
//  }
//
//  def parseWikiPage(page: WikiPage) = {
//    val parser = WikiParser.getInstance()
//    parser(page)
//  }
//
//  def buildSubjectURI(title: WikiTitle) = {
//     title.language.resourceUri.append(title.decodedWithNamespace)
//  }
//
//  def pageContext = new PageContext()
//
}