package extractors.parser

import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone
import de.unihd.dbs.uima.annotator.heideltime.resources.Language
import de.unihd.dbs.heideltime.standalone.DocumentType
import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.dataparser.DataParser
import scala.xml.XML
import org.apache.uima.UIMAFramework
import org.apache.uima.util.XMLInputSource
import de.unihd.dbs.heideltime.standalone.Config
import de.unihd.dbs.heideltime.standalone.components.impl._
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime
import java.util.Properties
import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger
import scala.Some
import ch.weisenburger.dbpedia.extraction.mappings.RuntimeAnalyzer
import org.slf4j.LoggerFactory


case class UnexpectedNumberOfTimexesException(numberOfTimexes: Int, timexes: Seq[String]) extends Exception


object AdvancedTimexParser {
  HeidelTimeStandalone.readConfigFile("resources/config.props")

  val jcasFacotry = synchronized {
    initJacasFactory
  }
  val uimaContext = synchronized {
    new UimaContextImpl(Language.ENGLISH, DocumentType.NARRATIVES)
  }
  val posTagger = synchronized {
    createPosTagger
  }

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
  val logger = LoggerFactory.getLogger(classOf[AdvancedTimexParser])

  val heidelTime = new HeidelTime();
  heidelTime.initialize(AdvancedTimexParser.uimaContext);

  val jcas = AdvancedTimexParser.jcasFacotry.createJCas

  val posTagger = AdvancedTimexParser.posTagger

  val xmlParser = AdvancedTimexParser.createXMLParser
  val resultFormatter = new TimeMLResultFormatter


  def parse(node: Node): Option[Any] = {
    assert(1 == 0);
    Some(0)
  }

  override def parse(node: Node, subjectUri: String): Option[Any] = {
    val stringValue = TimexParserCustomStringParser.parse(node)

    try {
      val result = stringValue match {
        case Some(str) => Some(parseDates(str, subjectUri))
        case None => None
      }

      //Logging
      if (logger.isErrorEnabled) result match {
        case None => logger.trace(s"Unable to parse node $node of $subjectUri to String");
        // no timex is nothing special -> lots of non temporal properties -> trace only
        case Some((None, None)) => logger.trace(s"No timex found in node $node, with value: ${stringValue.get}")
        // a timex is also nothing special --> see above
        case Some((fromTimex, None)) => logger.trace(s"Timex '$fromTimex' found in node $node, with value: ${stringValue.get}")
        case Some((fromTimex, toTimex)) => logger.trace(s"Timex interval '$fromTimex' - '$toTimex' found in node $node, with value: ${stringValue.get}")
      }

      result
    } catch {
      case unexno: UnexpectedNumberOfTimexesException => {
        logger.error(s"Found ${unexno.numberOfTimexes} timexes in $node. Unintended parsing result is likely. Timexes: ${unexno.timexes.mkString("; ")}; Node Value: ${stringValue.getOrElse("")}")
        None
      }
      case saxe: org.xml.sax.SAXParseException => {
        logger.error("", saxe)
        None
      }
    }
  }


  /** Parse for timexes on an infobox attribute string
    *
    * It is assumed, that a single timex denotes the start date / date when something occurred,
    * whereas two timexes denote an interval.
    *
    * More than two timexes most likely indicate a pareser/semantic error and therefore cause an exception
    *
    * @param str infobox property value
    * @param subjectUri the articles name
    * @return if found tuple of start and end timexes, else None
    */
  def parseDates(str: String, subjectUri: String): (Option[String], Option[String]) = {

    val analyzer = RuntimeAnalyzer(subjectUri)

    analyzer.jcas {
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

      val xml = xmlParser.loadString(timeMLString)

      val timexes = (xml \\ "TIMEX3")
      timexes.length match {
        case 0 => (None, None)
        case 1 => (getTimexValue(timexes.head), None)
        case 2 => (getTimexValue(timexes.head), getTimexValue(timexes.last))
        case i if i > 2 =>
          throw new UnexpectedNumberOfTimexesException(i, timexes.map(n => (n \\ "@value").toString))
      }
    }
  }


  /**
   * Ensures only timexes which have a year component are used
   * -> Currently suffices for company dataset

   * @param timexNode
   * @return
   */
  def getTimexValue(timexNode: xml.Node): Option[String] = {
    var str = (timexNode \\ "@value").toString
    if(str.length >= 4) {
      str = str.substring(0,4)
    } else {
      logger.error("Timex with less than the year: " + str + " in : " + timexNode.toString)
    }

    if(str.contains("X")) {
      logger.error("Year component unknown: " + str + " in : " + timexNode.toString )
      None
    } else {
      return Some(str)
    }
  }

  private def escapeStr(str: String) = {
    // result will be an XML -> escape predefined XML symbols
    str.replaceAll("&", "&amp;")
  }
}
