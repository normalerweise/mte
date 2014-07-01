package playground

import org.sweble.wikitext.engine.{PageId, PageTitle, Compiler}
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration
import it.uniroma1.lcl.wiki.parser.sweble.TextConverter

/**
 * Created by Norman on 08.05.14.
 */
object TextExtractor extends App {

  val content = "He followed this research by calling on the [[Chancellor of the Exchequer]] [[George Osborne]] to force these multinationals, which also included [[Google]] and [[The Coca-Cola Company]], to state the effective rate of tax they pay on their UK revenues. Elphicke also said that government contracts should be withheld from multinationals who do not pay their fair share of UK tax.<ref>{{cite news|last=Ebrahimi|first=Helia|title=Foreign firms could owe UK £11bn in unpaid taxes|url=http://www.telegraph.co.uk/finance/personalfinance/consumertips/tax/9652516/Foreign-firms-could-owe-UK-11bn-in-unpaid-taxes.html|newspaper=Telegraph|date=November 2, 2012}}</ref>\n\n===Charitable causes===\nAs of 2012, Apple is listed as a partner of the [[Product RED]] campaign, together with other brands such as Nike, Girl, American Express and Converse. The campaign's mission is to prevent the transmission of [[HIV]] from mother to child by 2015 (its byline is \"Fighting For An AIDS Free Generation\").<ref>{{cite web|title=(RED) Partners|url=http://www.joinred.com/aboutred/red-partners/|work=(RED)|publisher=(RED), a division of The ONE Campaign|accessdate=October 13, 2012|year=2012}}</ref>\n\nIn November 2012, Apple donated $2.5 million to the [[American Red Cross]] to aid relief efforts after [[Hurricane Sandy]].<ref>{{cite web|last=Weintraub|first=Seth|title=Apple donates $2.5M to Hurricane Sandy relief|url=http://9to5mac.com/2012/11/09/apple-donates-2-5-million-to-hurricane-sandy-relief/|publisher=9to5Mac|accessdate=November 18, 2012}}</ref>\n\n==See also==\n{{Wikipedia books|Apple Inc.}}\n"

  // set-up a simple wiki configuration
  val wikiConfig = new SimpleWikiConfiguration(
    "resources/config/sweble/SimpleWikiConfiguration."+"EN"+".xml")
  // instantiate a compiler for wiki pages
  val compiler = new Compiler(wikiConfig)


  val pageTitle = PageTitle.make(wikiConfig, "Apple_Inc.")
  val pageId = new PageId(pageTitle, "234234".toInt)
  val wikitext = content


  val cp = compiler.postprocess(pageId, wikitext, null);
  val p = new TextConverter(wikiConfig, 80);
  p.go(cp.getPage()) match {
    case text: String => println(text)
    case x => println("unknown text extr result: " + x.toString)
  };



}
