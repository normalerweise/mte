package playground

import ch.weisenburger.uima.FinancialDataPipelineFactory
import models.TextRevision
import org.specs2.mutable.Specification
import play.api.test.WithApplication

import scala.concurrent.Await
import scala.concurrent.duration._


class TextExtractionTest extends Specification {

  "Extract" should {
    "fin history" in new WithApplication {
      val pipeline = FinancialDataPipelineFactory.createSampleExctractionScalaCaseClassPipeline
      val revisions = Await.result(TextRevision.getPageRevs("Siemens"),Duration(30, SECONDS))
      val frevs = revisions.filter(_.id == 180455130)

      val quads = frevs.flatMap( rev => pipeline.process(rev.content.get, "http://dbpedia.org/resource/Siemens", 180455130, "")._1 )





      //TurtleSaver.initDestination("data/test.tt", quads)
      println(quads.map(_.sentenceText)mkString("\n"))
      println("done")
    }

  }
}
