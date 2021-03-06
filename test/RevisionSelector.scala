import extraction.download.{WikipediaRevisionSelector, WikipediaClient}
import models.Revision
import org.specs2.mutable.Specification
import play.api.test.WithApplication

/**
 * Created by Norman on 05.05.14.
 */
class RevisionSelector  extends Specification {

    "Extract" should {
      "fin history" in new WithApplication {

        //val revisions = RelevantRevisionDownloader.getRevisionStream("Apple_Inc.").toList
        //val selectedRevisions = RelevantRevisionDownloader.selectExtractionRelevantRevisions(revisions)
        val revisions =  WikipediaRevisionSelector.getRevisionsForExtraction("HiGear",
          WikipediaRevisionSelector.revisionsAtQuartilesPlusLatestForEachYear)

        val mhh =  WikipediaClient.downloadRevisionContents(revisions, "HiGear", "HiGear")

        println( revisions.sortBy(_.timestamp.toDate).reverse.take(5).map(_.timestamp).mkString("\n") )
        //println("")
       // println( selectedRevisions.sortBy(_.timestamp.toDate).reverse.take(5).mkString("\n") )

      }
    }
}
