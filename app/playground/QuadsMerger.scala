package playground

import models.Quad

object QuadsMerger {

  val groupByTemporalInformation = (quads: Seq[Quad]) => {
    quads.groupBy { q =>
      val from = q.context.get("fromDate")
      val to = q.context.get("toDate")
      from.getOrElse("") + to.getOrElse("")
    }
  }

  // all quads have same subject prediacte and temporal information
  val selectQuadsByValue: (Seq[Quad]) => Quad = (quads: Seq[Quad]) => quads.length match {
    case 1 => quads(0) // return the single quad
    case 2 =>
      // Either the values differ or it does not matter which one we choose.
      // Hard to state provenance, assumption => later (higher) revisions are more reliable
      quads.sortBy(_.context.get("sourceRevision").getOrElse("-1").toInt).last
    case x if x > 2 =>
      // Find the most frequent value, keep it simple -> in case of multiple values with highest frequency take the first
      val sorted = quads.groupBy(_.obj).toSeq.sortBy(_._2.length * -1)

      val mostFrequent = sorted.headOption.get // one value must exist
      val secondMostFrequent = sorted.lift(1).getOrElse(("", List.empty))

      if(mostFrequent._2.length <= secondMostFrequent._2.length) {
       println("Competing quad values:" + mostFrequent + ";" + secondMostFrequent)
      }
      mostFrequent._2.head
  }

  // assumes quadsOfWikiPage have the same subject
  def getDistinctQuads(quadsOfWikiPage: Seq[Quad]): Seq[Quad] = {
    val groupedQuads = quadsOfWikiPage.groupBy(_.predicate)
    val result = groupedQuads.map { quads =>
      val temporallyGroupedQuads = groupByTemporalInformation(quads._2)
      val selectedQuads = temporallyGroupedQuads.map( q => selectQuadsByValue(q._2))
      selectedQuads.toSeq
    }
    result.flatten.toSeq
    }

}
