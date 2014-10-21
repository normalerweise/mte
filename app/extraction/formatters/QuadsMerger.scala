package extraction.formatters

import extraction.OntologyUtil
import models.Quad

object QuadsMerger {


 private val NonTemporal = "non_temporal"


  // filter non-temporal value if temporal value is present and select Ontology Predicates only
  def mergeTemporallyConsistent(quadsOfWikiPage: Seq[Quad]): Seq[Quad] = {
    val groupedQuads = quadsOfWikiPage.groupBy(_.predicate)

    val result = groupedQuads.map {
      case (relation, quads) if OntologyUtil.isTemporal1to1Predicate(relation) =>
        val temporallyGroupedQuads = quads groupBy fromDateValue
        val (nonTemporal, temporal) = splitInNonTemporalAndTemporalParts(temporallyGroupedQuads)

        val latestNonTemporalQuad = if(nonTemporal.isEmpty) None else Some(selectLatestQuad(nonTemporal))

        if(temporal.isEmpty) {
          Seq(latestNonTemporalQuad.get)
        } else {
          val selectedTemporalQuads = temporal.map {
            case (_, quads) => selectLatestQuadOfMostFrequentValue(quads)
          }.toSeq

          val latestTemporalQuad = selectLatestQuad(selectedTemporalQuads)
          
          if(latestNonTemporalQuad.isDefined &&
            latestNonTemporalQuad.get.sourceRevisionAsNum > latestTemporalQuad.sourceRevisionAsNum) {
            // non-temporal quad is newer than temporal quad
            (selectedTemporalQuads :+ latestNonTemporalQuad.get)
          } else {
            selectedTemporalQuads
          }
        }

      case (relation, quads) if OntologyUtil.isOntologyPredicate(relation) =>
        selectLatestQuadPerValue(quads)

      case (relation, quads) =>
        // We don't know how to merge this relation in a temporally consistent manner
        throw UndefinedTemporalPropertyException()
    }
    result.flatten.toSeq
  }


  def getDistinctQuadsPerValueAndTemporalInformation(quadsOfWikiPage: Seq[Quad]): Seq[Quad] = {
    val groupedQuads = quadsOfWikiPage.groupBy(q =>
      q.predicate + q.obj + q.fromDate.getOrElse(NonTemporal) + q.toDate.getOrElse(NonTemporal))

    groupedQuads.map { case (_, quads) =>
      selectLatestQuad(quads)
    }.toSeq
  }



  private val groupByTemporalInformation = (quads: Seq[Quad]) => {
    quads.groupBy { q =>
      val from = q.fromDate
      val to = q.toDate
      from.getOrElse("") + to.getOrElse("")
    }
  }

  private val fromDateValue = (q: Quad) => q.fromDate.getOrElse(NonTemporal)

  private val maxRev = (a: Quad, b: Quad) => if (a.sourceRevisionAsNum > b.sourceRevisionAsNum) a else b
  private def selectLatestQuad(quads: Seq[Quad]) = quads reduce maxRev

  private def selectLatestQuadPerValue(quads: Seq[Quad]) =
    quads.groupBy(_.obj).map { case (_, quadsOfValue) => selectLatestQuad(quadsOfValue) }.toSeq


  // all quads have same subject prediacte and temporal information
  private def selectLatestQuadOfMostFrequentValue(quads: Seq[Quad]) = quads.length match {
    case 1 => quads(0) // return the single quad
    case x =>
      // Find the most frequent value, keep it simple -> in case of multiple values with highest frequency take the first
      val sorted = quads.groupBy(_.obj).toSeq.sortBy{ case (value, quads) => quads.length * -1}
      val mostFrequent = sorted.headOption.get._2 // one value must exist
      val secondMostFrequent = sorted.lift(1).getOrElse(("", List.empty))._2

      // competing quad values -> more trust in later revisions
      if (mostFrequent.length > secondMostFrequent.length) {
        selectLatestQuad(mostFrequent)
      } else if (mostFrequent.length < secondMostFrequent.length) {
        selectLatestQuad(secondMostFrequent)
      } else {
        // frequency is equal, later revision decides
        selectLatestQuad(mostFrequent ++ secondMostFrequent)
      }
  }

  private def splitInNonTemporalAndTemporalParts(temporallyGroupedQuads: Map[String, Seq[Quad]]) = {
    val (nonTemporal, temporal) = temporallyGroupedQuads.partition {
      case (year, _) => year == NonTemporal
    }

    val nonTemporalQuads = nonTemporal flatMap {
      case (_, quads) => quads
    }

    (nonTemporalQuads.toSeq, temporal.toSeq)
  }

  case class UndefinedTemporalPropertyException() extends Exception
}


