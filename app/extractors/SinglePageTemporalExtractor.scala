package extractors
//import extractors.Quad
//import scala.xml.XML
//import scala.concurrent.duration._
//import org.joda.time.DateTime
//import ch.weisenburger.dbpedia.extraction.mappings.RuntimeAnalyzer
//import play.api.libs.json.Json
//import java.io.FileNotFoundException
//import play.api.Logger
//
//
object SinglePageTemporalExtractor extends App {
//  val qd = extractPage("SAP_AG")
//  extractPage("Apple_Inc.")
//  printQuads(qd)
//  printRuntime
//
  def extractPage(pageName: String) = try {
//    val pages = loadPages("data/pages_over_time/" + pageName + ".xml")
//
//    val extractedQuads = extractTriples(pages)
//
//    extractedQuads
//  } catch {
//    case ex: FileNotFoundException =>
//      Logger.error("DiD not find XML for " + pageName, ex)
//      Seq.empty[Quad]
  Seq.empty[Quad]
  }
//
//  def printRuntime = {
//    for (a <- RuntimeAnalyzer.all) yield {
//      val duration = Duration(a.getTotalRuntime, MILLISECONDS);
//      println(a.page)
//      println("millis: " + a.getTotalRuntime);
//      println("WikiParser:" + a.getWikiParserRuntime._1 + " (" + a.getWikiParserRuntime._2 + ")")
//      println("Heideltime:" + a.getHeidelTimeRuntime._1 + " (" + a.getHeidelTimeRuntime._2 + ")")
//      println("POS Tagger:" + a.getPosTaggeRuntime._1 + " (" + a.getPosTaggeRuntime._2 + ")")
//      println("DBPedia Extractor" + a.getExtractorRuntime._1 + " (" + a.getExtractorRuntime._2 + ")")
//      println("Timex Formatter" + a.getTimexFormaterRuntime._1 + " (" + a.getTimexFormaterRuntime._2 + ")")
//      println("Jcas" + a.getJcasRuntime._1 + " (" + a.getJcasRuntime._2 + ")")
//    }
//    RuntimeAnalyzer.clear
//  }
//
//  def printQuads(quads: Seq[Quad]) = {
//    val sortedQuads = quads.sortBy(q => q.subject + q.predicate)
//    for (quad <- sortedQuads) yield println(quad.subject + " - " + quad.predicate + " - " + quad.obj + ": " + quad.context.mkString(", "))
//  }
//
//  def loadPages(filePath: String) = {
//    val pagesXml = XML.loadFile(filePath)
//    (pagesXml \\ "page")
//      .map(p => PageBuilder.fromPageNodeWithRevisionContent(p))
//  }
//
//  def extractTriples(pages: Seq[PageWithRevisionsContent]) = {
//    val quads = pages.map(extractTriplesFromPage(_)).flatten
//    // FIXME: Bad solution -> prevent null values
//    quads.map {
//      case q =>
//        val cleanedContext = q.context.filter(e => e._2 != null)
//        q.copy(context = cleanedContext)
//    }
//  }
//
//  def extractTriplesFromPage(page: PageWithRevisionsContent) = {
//    TemporalMappingExtractorWrapper.extract(page)
//  }
//
}