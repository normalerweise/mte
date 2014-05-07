package extractors.download

import models.Revision

object WikipediaRevisionSelector {

  val revisionsAtQuartilesPlusLatestForEachYear: (Seq[Revision]) => Seq[Revision] = (revisions) => {
    revisions.groupBy(_.timestamp.year.get)
      .flatMap { case (_,revisions) => revisionsAtQuartilesPlusLatest(revisions) }.toSeq
  }

  val revisionsAtQuartilesPlusLatest: (Seq[Revision]) => Seq[Revision] = (revisions) => {
    val length = revisions.length

    // the strategy selects 4 revisions with the objective to skip revisions without 'omitting relevant triples',
    // if we have less revisions simply use all
    if (length > 4) {
      // ensure revisions are sorted from old to new,
      // assumes revision ids are ascending over time
      val sortedRevs = revisions.sortBy(_.id)

      val median = medianOf(length)
      val firstQuartile = firstQuartil(length)
      val thirdQuartile = 3 * firstQuartile
      val latest = length - 1

      Seq(sortedRevs(firstQuartile), sortedRevs(median), sortedRevs(thirdQuartile), sortedRevs(latest))
    } else {
      revisions
    }
  }


  def getRevisionsForExtraction(articleNameInUrl: String, selectionStrategy: Seq[Revision] => Seq[Revision]) = {
    val allRevisions = WikipediaClient.enumerateRevisions(articleNameInUrl)
    selectionStrategy.apply(allRevisions)
  }


  private def firstQuartil(n: Int) = (n + 1) / 4

  private def medianOf(n: Int) = n match {
    // length is even or odd?
    case n: Int if n % 2 == 0 => (n + 1) / 2
    case n: Int => n / 2 // skip ((n)/2 + 1) => revs are discrete
  }

}
