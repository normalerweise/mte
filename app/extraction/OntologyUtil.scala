package extraction

import models.Quad

/**
 * Created by Norman on 27.05.14.
 */
object OntologyUtil {

  /** Specific conversion for (non-temporal) ontology predicates
    *
    */
  val temporalPredicates = Map(
    "http://dbpedia.org/ontology/numberOfEmployees" -> true,
    "http://dbpedia.org/ontology/revenue" -> true,
    "http://dbpedia.org/ontology/assets" -> true,
    "http://dbpedia.org/ontology/equity" -> true,
    "http://dbpedia.org/ontology/operatingIncome" -> true,
    "http://dbpedia.org/ontology/netIncome" -> true)

  val ontologyPredicates = Map("http://www.w3.org/1999/02/22-rdf-syntax-ns#type" -> true)

  def isTemporalPredicate(predicateUri: String) = temporalPredicates.get(predicateUri).isDefined
  
  def isOntologyPredicate(predicateUri: String) = ontologyPredicates.get(predicateUri).isDefined

}
