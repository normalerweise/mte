package playground

import org.dbpedia.extraction.wikiparser.TextNode
import extractors.AdvancedTimexParser
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
 * Created by Norman on 03.04.14.
 */
object TestTimexParser extends App {


//  val parser = new AdvancedTimexParser()
//
//  val node  = TextNode("blah 1940",1)
//  val result = parser.parse(node, "blah")
//
//  println(result)

  //val dt = new DateTime();
  //val fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
  //val  dt = fmt.parseDateTime("2010");
  val str ="2010-10"
  println(str.take(4))


}