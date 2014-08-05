package extraction.extractors

import java.io.File

import extraction.parser.AdvancedTimexParser
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{FileSource, XMLSource}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, PageNode, WikiParser}

/**
 * Created by Norman on 02.04.14.
 */
object DBpediaExtractorFrameworkContextFactory {

  type Context =  scala.AnyRef {
    def ontology: Ontology
    def language: Language
    def mappingPageSource: Traversable[org.dbpedia.extraction.sources.WikiPage]
    def mappings : Mappings
    def redirects : Redirects
    def timexParser: AdvancedTimexParser
  }

  private val mappingPageSources = scala.collection.mutable.Map.empty[Language, Traversable[org.dbpedia.extraction.sources.WikiPage]]
  private val redirects = scala.collection.mutable.Map.empty[Language, Redirects]

  // Ontology is language independent and can be reused
  lazy val dbPediaOntology = createOntology



  def createContextWithTimexParser(lang: Language, timexParser: AdvancedTimexParser): Context = synchronized {
    val _lang = lang
    val _ontology = dbPediaOntology
    val _redirects = getRedirects(lang)
    val _mappingPageSource = getMappingPageSource(lang)
    val _timexParser = timexParser

    new {

      private lazy val _mappings =
      {
        MappingsLoader.load(this)
      }

      def ontology: Ontology = _ontology
      def language: Language = _lang
      def redirects: Redirects = _redirects
      def mappingPageSource: Traversable[org.dbpedia.extraction.sources.WikiPage] = _mappingPageSource
      def mappings: Mappings = _mappings
      def timexParser: AdvancedTimexParser = _timexParser
    }
  }

  private def createOntology = {
    val ontologySource = XMLSource.fromFile(new File("resources/ontology.xml"), Language.Mappings)
    new OntologyReader().read(ontologySource)
  }

  private def createRedirects(lang: Language) = {
    // won't be used -> redirects will be loaded from cache file
    val dummySource: FileSource = null //  new FileSource(new File("dummy"), lang)
    val cacheFile = new File("resources/redirects/enwiki-20140502-template-redirects.obj")
    val redirects = Redirects.load(dummySource, cacheFile, lang)
    redirects
  }

  private def createMappingPageSource(lang: Language)  = {
    val namespace = Namespace.mappings(lang)
    val mappingsFile = new File("resources/mappings", namespace.name(Language.Mappings).replace(' ', '_') + ".xml")
    val mappingSource = XMLSource.fromFile(mappingsFile, Language.Mappings)

    mappingSource
  }

  private def getMappingPageSource(lang: Language): Traversable[org.dbpedia.extraction.sources.WikiPage] = synchronized {
    mappingPageSources.getOrElse(lang, {
      val ps = createMappingPageSource(lang)
      mappingPageSources(lang) = ps
      ps
    })
  }

  private def getRedirects(lang: Language) = synchronized {
    redirects.getOrElse(lang, {
      val r = createRedirects(lang)
      redirects(lang) = r
      r
    })
  }

}
