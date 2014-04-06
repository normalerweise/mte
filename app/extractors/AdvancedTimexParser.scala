package extractors

import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone
import de.unihd.dbs.uima.annotator.heideltime.resources.Language
import de.unihd.dbs.heideltime.standalone.{ DocumentType, OutputType }
import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.dataparser.{ DataParser, StringParser }
import scala.xml.XML
import org.dbpedia.extraction.wikiparser.TemplateNode
import de.unihd.dbs.heideltime.standalone.POSTagger
import org.apache.uima.resource.metadata.TypeSystemDescription
import org.apache.uima.UIMAFramework
import org.apache.uima.util.XMLInputSource
import de.unihd.dbs.heideltime.standalone.Config
import de.unihd.dbs.heideltime.standalone.components.impl._
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime
import java.util.Properties
import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger
import scala.Some
import ch.weisenburger.dbpedia.extraction.mappings.RuntimeAnalyzer

object AdvancedTimexParser {
  HeidelTimeStandalone.readConfigFile("resources/config.props")

  val jcasFacotry = synchronized{ initJacasFactory }
  val uimaContext = synchronized{ new UimaContextImpl(Language.ENGLISH, DocumentType.NARRATIVES) }
  val posTagger  = synchronized{ createPosTagger }

  def createXMLParser = {
    val f = javax.xml.parsers.SAXParserFactory.newInstance()
    f.setValidating(false)
    f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    val p = f.newSAXParser()
    val xmlParser = XML.withSAXParser(p)
    xmlParser
  }

  def initJacasFactory = {
    val descriptions = Array(
      UIMAFramework
        .getXMLParser()
        .parseTypeSystemDescription(
          new XMLInputSource(
            this.getClass()
              .getClassLoader()
              .getResource(
                Config.get(Config.TYPESYSTEMHOME)))),
      UIMAFramework
        .getXMLParser()
        .parseTypeSystemDescription(
          new XMLInputSource(
            this.getClass()
              .getClassLoader()
              .getResource(
                Config.get(Config.TYPESYSTEMHOME_DKPRO)))));
    val jcasFactory = new JCasFactoryImpl(descriptions)
    jcasFactory
  }

  def createPosTagger = {
    val settings = new Properties();

    val partOfSpeechTagger = new StanfordPOSTaggerWrapper();
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_ANNOTATE_TOKENS, Boolean.box(true));
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_ANNOTATE_SENTENCES, Boolean.box(true));
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_ANNOTATE_POS, Boolean.box(true));
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_MODEL_PATH, Config.get(Config.STANFORDPOSTAGGER_MODEL_PATH));
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_CONFIG_PATH, Config.get(Config.STANFORDPOSTAGGER_CONFIG_PATH));

    partOfSpeechTagger.initialize(settings);
    partOfSpeechTagger
  }

}

class AdvancedTimexParser extends DataParser {
  println("Creating AdvancedTimex Parser")
  val heidelTime = new HeidelTime();
  heidelTime.initialize(AdvancedTimexParser.uimaContext);

  val jcas = AdvancedTimexParser.jcasFacotry.createJCas

  val posTagger = AdvancedTimexParser.posTagger

  val xmlParser = AdvancedTimexParser.createXMLParser
  val resultFormatter = new TimeMLResultFormatter


  def parse(node: Node): Option[Any] = {
    assert(1 == 0); Some(0)
  }

  override def parse(node: Node, templateNode: String): Option[Any] = {
    val stringResult = StringParser.parse(node)
    stringResult match {
      case Some(str) => parseDates(str, templateNode)
      case None => None
    }
  }

  def parseDates(str: String, subjectUri: String): Option[Any] = {

    val analyzer = RuntimeAnalyzer(subjectUri)    

    analyzer.jcas {
      //AdvancedTimexParser.jcasFacotry.createJCas();
      jcas.reset()
      jcas.setDocumentText(escapeStr(str));
    }

    analyzer.startPosTagger
    posTagger.process(jcas);
    analyzer.stopPosTagger

    analyzer.startHeideltime
    heidelTime.process(jcas);
    analyzer.stopHeidelTime

    analyzer.formatTIMEX {
      val timeMLString = resultFormatter.format(jcas)

      try {

        val xml = xmlParser.loadString(timeMLString)

        val timexes = (xml \\ "TIMEX3")
        if (timexes.length == 0) {
          //println("No timex found for" + str);
          None
        } else if (timexes.length == 1) {
          //println(" timex found for" + str + ":" + timexes.head);
          Some((timexes.head \\ "@value").toString)
        } else if (timexes.length == 2) {
          println(" timex found for" + str + ": start" + timexes.head + "; end " + timexes.tail);
          Some((timexes.head \\ "@value").toString)
        } else {
          println("too much timexes found for " + str)
          None
        }

      } catch {
        case saxe: org.xml.sax.SAXParseException => {
          println("String which cause exception: " + timeMLString)
          saxe.printStackTrace
          None
        }
      }
    }
  }

  private def escapeStr(str: String) = {
    // result will be an XML -> escape predefined XML symbols
    str.replaceAll("&", "&amp;")
  }
}