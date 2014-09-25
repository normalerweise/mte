package extraction.parser

import ch.weisenburger.uima.FinancialDataPipelineFactory
import ch.weisenburger.uima.types.distantsupervision.skala.Timex
import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.dataparser.DataParser
import org.slf4j.LoggerFactory


case class UnexpectedNumberOfTimexesException(numberOfTimexes: Int, timexes: Seq[String]) extends Exception

class AdvancedTimexParser extends DataParser {
  val logger = LoggerFactory.getLogger(classOf[AdvancedTimexParser])

  val timexPipeline = FinancialDataPipelineFactory.createTimexExtractionScalaCaseClassPipeline

  def parse(node: Node): Option[Any] = {
    assert(1 == 0);
    Some(0)
  }

  override def parse( node : Node, dependentParserResult: Option[Any], subjectUri: String): Option[Any] = {
    val stringValue = TimexParserCustomStringParser.parse(node)
    val valueParserResult = dependentParserResult
    try {
      val result = stringValue match {
        case Some(str) => Some(parseDates(str, valueParserResult, subjectUri))
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
  def parseDates(str: String, valueParserResult: Option[Any], subjectUri: String): (Option[String], Option[String]) = {

      val dateTimexes = timexPipeline.process(str).filter(_.ttype == "DATE")

      // in case of a infobox property a timex which interferes with the property value
      // is likely to be a false positive
      val trueTimexes = valueParserResult match {
          // value parser result has type (value as Double, unit, value as String, unit as String)
        case Some((_, _, valueString: String, unitString)) =>
          val noOverlapWithValueParserResult = buildNoOverlapCheckFunction(str, valueString)
          dateTimexes.filter(noOverlapWithValueParserResult)
        case Some((doubleValue, valueString: String)) =>
          val noOverlapWithValueParserResult = buildNoOverlapCheckFunction(str, valueString)
          dateTimexes.filter(noOverlapWithValueParserResult)
        case _ => dateTimexes

      }

      trueTimexes.length match {
        case 0 => (None, None)
        case 1 => (getTimexValue(trueTimexes.head), None)
        case 2 =>
          // interval found -> store in temporal order
          val first = getTimexValue(trueTimexes.head)
          val second = getTimexValue(trueTimexes.last)
          (first,second) match {
            case (Some(first), Some(second)) if first <= second => (Some(first), Some(second))
            case (Some(first), Some(second)) if first > second => (Some(second), Some(first))
            case (None, Some(second)) => (Some(second), None)
            case (Some(first), None) => (Some(first), None)
            case (None, None) => (None, None)
          }
        case i if i > 2 =>
          throw new UnexpectedNumberOfTimexesException(i, trueTimexes.map(t => t.toString))
      }
    }



  /**
   * Ensures only timexes which have a year component are used
   * -> Currently suffices for company dataset

   * @param timex
   * @return
   */
  def getTimexValue(timex: Timex): Option[String] = timex.value match {
    case str if str.length < 4 =>
      logger.trace("Timex with less than the year: " + str + " in : " + timex.toString)
      None
    case str if str.substring(0,4).contains("X") =>
      logger.trace("Year component unknown: " + str + " in : " + timex.toString )
      None
    case str =>Some(str)
  }

//  /** Escapes the 5 predefined XML characters
//   *
//   * See http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
//   * "  &quot;
//   * '   &apos;
//   * <   &lt;
//   * >   &gt;
//   * &   &amp;
//   *
//   * @param str
//   * @return escaped string
//   */
//  private def escapePredefinedXMLCharacters(str: String) = {
//    // result will be an XML -> escape predefined XML symbols
//    val sb = new StringBuilder()
//    for(i <- 0 until str.length) {
//      str.charAt(i) match {
//        case '"' => sb.append("&quot;")
//        case '\'' => sb.append("&apos;")
//        case '<' => sb.append("&lt;")
//        case '>' => sb.append("&gt;")
//        case '&' => sb.append("&amp;")
//        case c => sb.append(c)
//      }
//    }
//    sb.toString
//  }

  private def sizeOf(o: Object, s: String) = {
  //  logger.error(s +  ": " + SizeOf.humanReadable(SizeOf.deepSizeOf(o)))
  }

  def buildNoOverlapCheckFunction(str: String, valueString: String) = {
    val valueBegin = str.indexOf(valueString)
    val valueEnd = valueBegin + valueString.length
    valueBegin match {
      case -1 => (timex: Timex) => true // don't filter anything if value string can't be matched
      case _ => (timex: Timex) => timex.end < valueBegin || timex.begin > valueEnd
    }

  }


}
