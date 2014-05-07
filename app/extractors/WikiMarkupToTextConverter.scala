package extractors

import org.sweble.wikitext.engine.{PageId, PageTitle, Compiler}
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration
import it.uniroma1.lcl.wiki.parser.sweble.TextConverter


object WikiMarkupToTextConverter {

    // set-up a simple wiki configuration
    val wikiConfig = new SimpleWikiConfiguration(
      "resources/config/sweble/SimpleWikiConfiguration."+"EN"+".xml")
    // instantiate a compiler for wiki pages
    val compiler = new Compiler(wikiConfig)


    def convert(wikiMarkupText: String) = {
      val pageTitle = PageTitle.make(wikiConfig, "dummy")
      val pageId = new PageId(pageTitle, 0)

      val cp = compiler.postprocess(pageId, wikiMarkupText, null);
      val p = new TextConverter(wikiConfig, 80);
      p.go(cp.getPage()) match {
        case text: String => text
        case x => throw new Exception("unexpected wiki markup conversion result: " + x)
      }
    }
}
