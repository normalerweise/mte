package extractors

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.wikiparser.{WikiParser, Namespace, PageNode}
import org.dbpedia.extraction.sources.XMLSource
import java.io.File
import org.dbpedia.extraction.ontology.io.OntologyReader

/**
 * Created by Norman on 02.04.14.
 */
object DBpediaExtractorFrameworkContextFactory {

  type Context =  scala.AnyRef {
    def ontology: Ontology
    def language: Language
    def mappingPageSource: Traversable[PageNode]
    def mappings : Mappings
    def redirects : Redirects
    def timexParser: AdvancedTimexParser
  }

  private val mappingPageSources = scala.collection.mutable.Map.empty[Language, Traversable[PageNode]]

  // Ontology is langauge independent and can be reused
  lazy val dbPediaOntology = createOntology
  lazy val redirects = createRedirects



  def getMappingPageSource(lang: Language): Traversable[PageNode] = synchronized {
    mappingPageSources.getOrElse(lang, {
      val ps = createMappingPageSource(lang)
      mappingPageSources(lang) = ps
      ps
    })
  }


  def createContext(lang: Language): Context = {
    val _lang = lang
    val _ontology = dbPediaOntology
    val _redirects = redirects
    val _mappingPageSource = getMappingPageSource(lang)
    val _timexParser = new AdvancedTimexParser

    new {
      def ontology: Ontology = _ontology
      def language: Language = _lang
      def redirects: Redirects = _redirects
      def mappingPageSource: Traversable[PageNode] = _mappingPageSource
      def mappings: Mappings = MappingsLoader.load(this)
      def timexParser: AdvancedTimexParser = _timexParser
    }
  }




  def createOntology = {
    val ontologySource = XMLSource.fromFile(new File("resources/ontology.xml"), Language.Mappings)
    new OntologyReader().read(ontologySource)
  }

  def createRedirects = {
    // TODO simple version without previous redirect file
    new Redirects(Map())
  }

  def createMappingPageSource(lang: Language)  = {
    val parse = WikiParser.getInstance()
    val namespace = Namespace.mappings(lang)
    val mappingsFile = new File("resources/mappings", namespace.name(Language.Mappings).replace(' ', '_') + ".xml")
    XMLSource.fromFile(mappingsFile, Language.Mappings).map(parse)
  }

}
