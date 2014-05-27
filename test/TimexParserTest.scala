import actors.Util
import extraction.extractors.{TemporalDBPediaMappingExtractorWrapper, ThreadUnsafeDependencies}
import extraction.formatters.TurtleSaver
import org.joda.time.DateTime
import models.{Page, Revision}
import play.api.test.FakeApplication
import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable._
import scala.concurrent.Await
import scala.concurrent.duration._

class TimexParserTest extends Specification {

  "Extract" should {
    "fin history" in new WithApplication {
      val dependencies =  ThreadUnsafeDependencies.create("tester")
      val extractor = new TemporalDBPediaMappingExtractorWrapper(dependencies)

      val revisions = Await.result(Revision.getPageRevs("Apple_Inc."), Duration(5000, MILLISECONDS))
      val frevs = revisions.filter(_.id == 521114821)
      val page = revisions.head.page.get

      val quads = frevs.flatMap( rev => extractor.extract(rev))





      TurtleSaver.save("data/test.tt", quads)
      println("done")
    }

  }
}


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
  //val str ="2010-10"
  //println(str.take(4))





}