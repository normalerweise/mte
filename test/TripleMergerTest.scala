import extraction.extractors.{TemporalDBPediaMappingExtractorWrapper, Dependencies}
import extraction.formatters.QuadsMerger
import org.joda.time.DateTime
import models.{Page, Revision}
import play.api.test.FakeApplication
import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable._

class TripleMergerTest extends Specification {

  "Extract" should {
    "fin history" in new WithApplication {


      val content1 =
        """
          |{{Infobox company
          || name = Apple Inc.
          || location_city = [[Apple Campus]], 1 [[Infinite Loop (street)|Infinite Loop]], [[Cupertino,
          || revenue = {{Increase}} US$ 170.910&nbsp;[[1000000000 (number)|billion]] (2013)<ref name="8-K-2013">{{cite web|url=http://investor.apple.com/secfiling.cfm?filingID=1193125-13-413498&CIK=320193 |title=2013 Apple Form 8-K |date= October 28, 2013|accessdate=October 29, 2013}}</ref>
          || operating_income = {{Decrease}} US$ {{0|0}}48.999&nbsp;billion (2013)<ref name="8-K-2013"/>
          || net_income = {{Decrease}} US$ {{0|0}}36.037&nbsp;billion (2013)<ref name="8-K-2013"/>
          || assets = {{increase}} US$ 200.000&nbsp;billion (2013)<ref name="8-K-2013"/>
          || equity = {{Increase}} US$ 123.549&nbsp;billion (2013)<ref name="8-K-2013"/>
          |}}
          |
        """.stripMargin

      val content2 =
        """
          |{{Infobox company
          || name = Apple Inc.
          || location_city = [[Apple Campus]], 1 [[Infinite Loop (street)|Infinite Loop]], [[Cupertino,
          || revenue = {{Increase}} US$ 160.910&nbsp;[[1000000000 (number)|billion]] (2012)<ref name="8-K-2013">{{cite web|url=http://investor.apple.com/secfiling.cfm?filingID=1193125-13-413498&CIK=320193 |title=2013 Apple Form 8-K |date= October 28, 2013|accessdate=October 29, 2013}}</ref>
          || operating_income = {{Decrease}} US$ {{0|0}}48.999&nbsp;billion (2013)<ref name="8-K-2013"/>
          || net_income = {{Decrease}} US$ {{0|0}}36.037&nbsp;billion (2013)<ref name="8-K-2013"/>
          || assets = {{increase}} US$ 200.000&nbsp;billion (2013)<ref name="8-K-2013"/>
          || equity = {{Increase}} US$ 123.549&nbsp;billion (2013)<ref name="8-K-2013"/>
          |}}
          |
        """.stripMargin

      val content3 =
        """
          |{{Infobox company
          || name = Apple Inc.
          || location_city = [[Apple Campus]], 1 [[Infinite Loop (street)|Infinite Loop]], [[Cupertino,
          || revenue = {{Increase}} US$ 150.910&nbsp;[[1000000000 (number)|billion]] (2011)<ref name="8-K-2013">{{cite web|url=http://investor.apple.com/secfiling.cfm?filingID=1193125-13-413498&CIK=320193 |title=2013 Apple Form 8-K |date= October 28, 2013|accessdate=October 29, 2013}}</ref>
          || operating_income = {{Decrease}} US$ {{0|0}}48.999&nbsp;billion (2013)<ref name="8-K-2013"/>
          || net_income = {{Decrease}} US$ {{0|0}}30.037&nbsp;billion (2013)<ref name="8-K-2013"/>
          || assets = {{increase}} US$ 207.000&nbsp;billion (2013)<ref name="8-K-2013"/>
          || equity = {{Increase}} US$ 123.549&nbsp;billion (2013)<ref name="8-K-2013"/>
          |}}
          |
        """.stripMargin

      val content4 =
        """
          |{{Infobox company
          || name = Apple Inc.
          || location_city = [[Apple Campus]], 1 [[Infinite Loop (street)|Infinite Loop]], [[Cupertino,
          || revenue = {{Increase}} US$ 140.910&nbsp;[[1000000000 (number)|billion]] (2010)<ref name="8-K-2013">{{cite web|url=http://investor.apple.com/secfiling.cfm?filingID=1193125-13-413498&CIK=320193 |title=2013 Apple Form 8-K |date= October 28, 2013|accessdate=October 29, 2013}}</ref>
          || operating_income = {{Decrease}} US$ {{0|0}}48.999&nbsp;billion (2013)<ref name="8-K-2013"/>
          || net_income = {{Decrease}} US$ {{0|0}}30.037&nbsp;billion (2013)<ref name="8-K-2013"/>
          || assets = {{increase}} US$ 207.000&nbsp;billion (2013)<ref name="8-K-2013"/>
          || equity = {{Increase}} US$ 123.549&nbsp;billion (2013)<ref name="8-K-2013"/>
          |}}
          |
        """.stripMargin





      val page = Page(0, "test", "test", "test", "test")
      val rev1 = Revision(1, new DateTime, page, content1)
      val rev2 = Revision(2, new DateTime, page, content2)
      val rev3 = Revision(3, new DateTime, page, content3)
      val rev4 = Revision(4, new DateTime, page, content4)


      val dependencies =  new Dependencies
      val extractor = new TemporalDBPediaMappingExtractorWrapper(dependencies)

      val result = extractor.extract(rev1) ++ extractor.extract(rev2) ++ extractor.extract(rev3) ++ extractor.extract(rev4)
      val mergedResult = QuadsMerger.getDistinctQuadsPerYear(result)
      //TurtleSaver.save(result)
      println(mergedResult.mkString("\n"))
    }

  }
}

