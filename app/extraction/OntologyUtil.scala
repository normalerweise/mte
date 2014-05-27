package extraction

import models.Quad

/**
 * Created by Norman on 27.05.14.
 */
object OntologyUtil {

  /** Specific conversion for (non-temporal) ontology predicates
    *
    */
  val ontologyPredicates = Map("http://www.w3.org/1999/02/22-rdf-syntax-ns#type" -> true)
  def isOntologyPredicate(predicateUri: String) = ontologyPredicates.get(predicateUri).isDefined

}
