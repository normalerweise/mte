package extraction.parser

import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone
import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger
import de.unihd.dbs.heideltime.standalone.components.impl.{TimeMLResultFormatter, JCasFactoryImpl, UimaContextImpl}
import de.unihd.dbs.uima.annotator.heideltime.resources.Language
import de.unihd.dbs.heideltime.standalone.DocumentType
import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.dataparser.DataParser
import scala.io.Source
import scala.xml.XML
import org.apache.uima.UIMAFramework
import org.apache.uima.util.XMLInputSource
import de.unihd.dbs.heideltime.standalone.Config
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime
import java.util.Properties
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

    val partOfSpeechTagger = new de.unihd.dbs.heideltime.standalone.components.impl.StanfordPOSTaggerWrapper();
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_ANNOTATE_TOKENS, Boolean.box(true));
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_ANNOTATE_SENTENCES, Boolean.box(true));
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_ANNOTATE_POS, Boolean.box(true));
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_MODEL_PATH, Config.get(Config.STANFORDPOSTAGGER_MODEL_PATH));
    settings.put(PartOfSpeechTagger.STANFORDPOSTAGGER_CONFIG_PATH, Config.get(Config.STANFORDPOSTAGGER_CONFIG_PATH));

    partOfSpeechTagger.initialize(settings);
    //partOfSpeechTagger.initialize(uimaContext)
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
        logger.error("SAXPE in Value: " + stringValue, saxe)
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


      jcas.reset()
      jcas.setDocumentText(escapePredefinedXMLCharacters(str));



    posTagger.process(jcas);



    heidelTime.process(jcas);



      val timeMLString = resultFormatter.format(jcas)

      val xml = xmlParser.loadString(timeMLString)

      val timexes = (xml \\ "TIMEX3").filter( n => (n \ "@type").text == "DATE" )
      timexes.length match {
        case 0 => (None, None)
        case 1 => (getTimexValue(timexes.head), None)
        case 2 =>
          // interval found -> store in temporal order
          val first = getTimexValue(timexes.head)
          val second = getTimexValue(timexes.last)
          (first,second) match {
            case (Some(first), Some(second)) if first <= second => (Some(first), Some(second))
            case (Some(first), Some(second)) if first > second => (Some(second), Some(first))
            case (None, Some(second)) => (Some(second), None)
            case (Some(first), None) => (Some(first), None)
            case (None, None) => (None, None)
          }
        case i if i > 2 =>
          throw new UnexpectedNumberOfTimexesException(i, timexes.map(n => (n \\ "@value").toString))
      }
    }



  /**
   * Ensures only timexes which have a year component are used
   * -> Currently suffices for company dataset

   * @param timexNode
   * @return
   */
  def getTimexValue(timexNode: xml.Node): Option[String] = (timexNode \\ "@value").toString match {
    case str if str.length < 4 =>
      logger.trace("Timex with less than the year: " + str + " in : " + timexNode.toString)
      None
    case str if str.substring(0,4).contains("X") =>
      logger.trace("Year component unknown: " + str + " in : " + timexNode.toString )
      None
    case str =>Some(str)
  }

  /** Escapes the 5 predefined XML characters
   *
   * See http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
   * "  &quot;
   * '   &apos;
   * <   &lt;
   * >   &gt;
   * &   &amp;
   *
   * @param str
   * @return escaped string
   */
  private def escapePredefinedXMLCharacters(str: String) = {
    // result will be an XML -> escape predefined XML symbols
    val sb = new StringBuilder()
    for(i <- 0 until str.length) {
      str.charAt(i) match {
        case '"' => sb.append("&quot;")
        case '\'' => sb.append("&apos;")
        case '<' => sb.append("&lt;")
        case '>' => sb.append("&gt;")
        case '&' => sb.append("&amp;")
        case c => sb.append(c)
      }
    }
    sb.toString
  }

  private def sizeOf(o: Object, s: String) = {
  //  logger.error(s +  ": " + SizeOf.humanReadable(SizeOf.deepSizeOf(o)))
  }


}
