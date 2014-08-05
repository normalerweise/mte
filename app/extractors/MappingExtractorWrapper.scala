package extractors

import java.io.File
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.wikiparser.{ PageNode, Namespace, WikiParser }

object DBPediaExtractorWrapper {
  val wikiParser = WikiParser.getInstance("simple")
  val extractors = scala.collection.mutable.Map.empty[Language, MappingExtractor]

  def extract(lang: Language, page : PageNode, subjectUri : String, pageContext : PageContext) : Seq[org.dbpedia.extraction.destinations.Quad] = {
    extractor(lang).extract(page, subjectUri, pageContext)
  }
  
  def extractor(lang: Language) = {
    extractors.getOrElse(lang, {
      val ex = initExtractor(lang)
      extractors(lang) = ex
      ex
    })
  }

  def initExtractor(lang: Language): MappingExtractor = {
    val _lang = lang
    val _ontology = loadOntology
    val _redirects = loadRedirects
    val _mappingPageSource = loadMappingPageSource(_lang)
    
    val context = new {
      def ontology: Ontology = _ontology
      def language: Language = _lang
      def redirects: Redirects = _redirects
      def mappingPageSource: Traversable[PageNode] = _mappingPageSource
      def mappings: Mappings = MappingsLoader.load(this)
    }

    new MappingExtractor(context)
  }

  def loadOntology = {
    val ontologySource = XMLSource.fromFile(new File("resources/ontology.xml"), Language.Mappings)
    new OntologyReader().read(ontologySource)
  }

  def loadRedirects = {
    // TODO simple version without previous redirect file
   new Redirects(Map())
  }

  def loadMappingPageSource(lang: Language)  = {
    val namespace = Namespace.mappings(lang)
    val mappingsFile = new File("resources/mappings", namespace.name(Language.Mappings).replace(' ', '_') + ".xml")
    XMLSource.fromFile(mappingsFile, Language.Mappings).map(wikiParser)
  }

}