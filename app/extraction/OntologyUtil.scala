package extraction

import models.Quad

/**
 * Created by Norman on 27.05.14.
 */
object OntologyUtil {

  val temporalPredicates = Map(
    "http://dbpedia.org/ontology/numberOfEmployees" -> true,
    "http://dbpedia.org/ontology/revenue" -> true,
    "http://dbpedia.org/ontology/assets" -> true,
    "http://dbpedia.org/ontology/equity" -> true,
    "http://dbpedia.org/ontology/operatingIncome" -> true,
    "http://dbpedia.org/ontology/netIncome" -> true,
    "http://dbpedia.org/ontology/team" -> true,
    "http://dbpedia.org/ontology/formerTeam" -> true)

  val ontologyPredicates = Map("http://www.w3.org/1999/02/22-rdf-syntax-ns#type" -> true)

  def isTemporal1to1Predicate(predicateUri: String) = temporalPredicates.get(predicateUri).isDefined
  
  def isOntologyPredicate(predicateUri: String) = ontologyPredicates.get(predicateUri).isDefined

}
