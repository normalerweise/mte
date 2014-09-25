package extraction.formatters

import models.Quad
import extraction.OntologyUtil

object QuadsMerger {

  val groupByTemporalInformation = (quads: Seq[Quad]) => {
    quads.groupBy { q =>
      val from = q.fromDate
      val to = q.toDate
      from.getOrElse("") + to.getOrElse("")
    }
  }

  val groupByFromDateTemporalInformation = (quads: Seq[Quad]) => {
    quads.groupBy { q =>
      val from = q.fromDate
      from.getOrElse("")
    }
  }

  // all quads have same subject prediacte and temporal information
  val selectQuadsByValue: (Seq[Quad]) => Quad = (quads: Seq[Quad]) => quads.length match {
    case 1 => quads(0) // return the single quad
    case 2 =>
      // Either the values differ or it does not matter which one we choose.
      // Hard to state 'trust', assumption => later (higher) revisions are more reliable
      quads.sortBy(_.sourceRevision.getOrElse("-1").toInt).last
    case x if x > 2 =>
      // Find the most frequent value, keep it simple -> in case of multiple values with highest frequency take the first
      val sorted = quads.groupBy(_.obj).toSeq.sortBy(_._2.length * -1)

      val mostFrequent = sorted.headOption.get // one value must exist
      val secondMostFrequent = sorted.lift(1).getOrElse(("", List.empty))

      // competing quad values -> more trust in later revisions
      if (mostFrequent._2.length <= secondMostFrequent._2.length) {
        val mostFrequentRev =
          mostFrequent._2.sortBy(_.sourceRevision.getOrElse("-1").toInt)
            .last.sourceRevision.getOrElse("-1").toInt
        val secondMostFrequentRev =
          secondMostFrequent._2.sortBy(_.sourceRevision.getOrElse("-1").toInt)
            .last.sourceRevision.getOrElse("-1").toInt

        if(mostFrequentRev > secondMostFrequentRev) {
          mostFrequent._2.head
        }else{
          secondMostFrequent._2.head
        }
      }else {
        mostFrequent._2.head
      }
  }

  // assumes quadsOfWikiPage have the same subject
  def getDistinctQuadsPerYear(quadsOfWikiPage: Seq[Quad]): Seq[Quad] = {
    val groupedQuads = quadsOfWikiPage.groupBy(_.predicate)
    val result = groupedQuads.map { quads =>
      if (OntologyUtil.isTemporalPredicate(quads._1)) {
        val temporallyGroupedQuads = groupByTemporalInformation(quads._2)
        val selectedQuads = temporallyGroupedQuads.map(q => selectQuadsByValue(q._2))
        selectedQuads.toSeq
      } else {
      selectOneQuadPerValue(quads._2)
    } 

    }
    result.flatten.toSeq
  }

  val maxRev = (a: Quad,b: Quad) => if(a.sourceRevision.get.toLong > b.sourceRevision.get.toLong) a else b

  // filter non-temporal value if temporal value is present and select Ontology Predicates only
  def getDistinctQuadsPerYearWithNonTemporalFilter(quadsOfWikiPage: Seq[Quad]): Seq[Quad] = {
    val groupedQuads = quadsOfWikiPage.groupBy(_.predicate)
    val result = groupedQuads.map { quads =>
      if (OntologyUtil.isTemporalPredicate(quads._1)) {
        val temporallyGroupedQuads = groupByFromDateTemporalInformation(quads._2)
        val selectedQuads =
          if(temporallyGroupedQuads.size == 1 && temporallyGroupedQuads.get("").isDefined) {
            // Select the latest value for non temporal quad
            Seq(temporallyGroupedQuads.head._2 reduce maxRev)
          } else {
            println(temporallyGroupedQuads)
            val (nonTemporal, temporal) = temporallyGroupedQuads.partition(_._1 == "")
            val temporalQuads = temporal.map(q => selectQuadsByValue(q._2))
            // get the latest non temporal quad
            if(nonTemporal.headOption.isDefined) {
              val nonTemporalQuad = nonTemporal.head._2 reduce maxRev

              val tempMaxRev = temporalQuads reduce maxRev
              if (nonTemporalQuad.sourceRevision.get.toLong > tempMaxRev.sourceRevision.get.toLong) {
                // non-temporal quad is newer than temporal quad
                (temporalQuads.toSeq :+ nonTemporalQuad)
              } else {
                temporalQuads
              }
            }else {
              temporalQuads
            }
          }
        selectedQuads.toSeq
      } else if (OntologyUtil.isOntologyPredicate(quads._1)) {
        selectOneQuadPerValue(quads._2)
      } else {
       Seq.empty
      }
    }
    result.flatten.toSeq
  }

  // assumes quadsOfWikiPage have the same subject
  def getDistinctQuadsPerValueAndTimex(quadsOfWikiPage: Seq[Quad]): Seq[Quad] = {
    val groupedQuads = quadsOfWikiPage.groupBy(q =>
      q.predicate + q.obj + q.context.get("fromDate").getOrElse("") + q.context.get("toDate").getOrElse(""))
    // select the first one
    val result = groupedQuads.map { case (_,quads) =>
      quads.head
    }
    result.toSeq
  }

  def selectOneQuadPerValue(quads: Seq[Quad])= {
    quads.groupBy(_.obj).map { case (_,quadsOfValue) => quadsOfValue.head }.toSeq
  }

}
