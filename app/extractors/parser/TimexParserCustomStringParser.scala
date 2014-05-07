package extractors.parser

import org.dbpedia.extraction.dataparser.DataParser
import org.dbpedia.extraction.wikiparser.{TableNode, TextNode, Node}
import scala.util.matching.Regex.Match
import org.dbpedia.extraction.util.WikiUtil

object TimexParserCustomStringParser extends DataParser {

  private val smallTagRegex = """<small[^>]*>\(?(.*?)\)?<\/small>""".r
  private val tagRegex = """\<.*?\>""".r

  override def parse(node: Node): Option[String] = {

    //Build text from node
    val sb = new StringBuilder()
    nodeToString(node, sb)

    //Clean text
    var text = sb.toString()
    // Replace text in <small></small> tags with an "equivalent" string representation
    // Simply extracting the content puts this data at the same level as other text appearing
    // in the node, which might not be the editor's semantics
    text = smallTagRegex.replaceAllIn(text, (m: Match) => if (m.group(1).nonEmpty) "($1)" else "")
    text = tagRegex.replaceAllIn(text, "") //strip tags
    text = WikiUtil.removeWikiEmphasis(text)
    text = text.replace("&nbsp;", " ") //TODO decode all html entities here
    text = text.trim

    if (text.isEmpty) {
      None
    }
    else {
      Some(text)
    }
  }

  private def nodeToString(node: Node, sb: StringBuilder) {
    node match {
      case TextNode(text, _) => sb.append(text)
      case _: TableNode => //ignore
      case _ => node.children.foreach(child => nodeToString(child, sb))
    }
  }
}
