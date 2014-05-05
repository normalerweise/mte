import actors.events.EventLogger
import models.EventTypes._
import models.{Event, ExtractionRunPageResult}
import org.specs2.mutable.Specification
import play.api.test.WithApplication
import playground.{TurtleSaver, QuadsMerger}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Created by Norman on 04.05.14.
 */
class QuadReader extends Specification {
  import scala.concurrent.duration._
  "Extract" should {
    "fin history" in new WithApplication {



      val  pageExtractionResults =  Await.result(ExtractionRunPageResult.get("5366259d570000ea0043bd7b"),  Duration(5000, MILLISECONDS))
          val quadsToSave = pageExtractionResults.map {
            page => QuadsMerger.getDistinctQuads(page.quads)
          }
          TurtleSaver.save(s"data/test_5366259d570000ea0043bd7b.tt", quadsToSave.flatten)
    }
  }
}