package playground

import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.util.Language

/**
 * Created by Norman on 18.06.14.
 */
object ResourceUrlTest extends App{
 val title = WikiTitle.parse("myArticle",Language("en"))


 val page = new WikiPage(title, "huhu")

  println(title.resourceIri)

}
