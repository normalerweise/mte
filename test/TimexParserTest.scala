import actors.Util
import extraction.extractors.{TemporalDBPediaMappingExtractorWrapper, Dependencies}
import extraction.formatters.SingletonPropertyTurtleSaver
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
      val dependencies =  new Dependencies()
      val extractor = new TemporalDBPediaMappingExtractorWrapper(dependencies)

      val revisions = Await.result(Revision.getPageRevs("Pirelli"), Duration(5000, MILLISECONDS))
      val frevs = revisions.filter(_.id == 511480441)
      val page = revisions.head.page.get

      val quads = frevs.flatMap( rev => extractor.extract(rev))





      //TurtleSaver.initDestination("data/test.tt", quads)
      println("done")
    }

  }
}
