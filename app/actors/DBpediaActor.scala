package actors


import akka.actor.Actor
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}
import models.Event
import models.EventTypes._
import actors.events.EventLogger

/**
 * Created by Norman on 31.03.14.
 */
case class QueryCompanies()
case class QueriedCompanies(companyUris: List[String])


class DBpediaActor extends Actor {

  def receive = {
    case QueryCompanies => sender ! QueriedCompanies(executeCompanyQuery)
  }

  private def executeCompanyQuery() = {
    val dbPediaSparqlEndpointUrlString = "http://dbpedia.org/sparql"

    def queryString(limit: Int, offset: Int) =
      s"""
        PREFIX res: <http://dbpedia.org/ontology/>

        SELECT distinct ?x
        WHERE
          {
            {
              SELECT DISTINCT ?x
              WHERE
                {
                  ?x a res:Company
                } ORDER BY ASC(?x)
            }
            FILTER NOT EXISTS
             { ?x res:wikiPageRedirects ?y }
          }
        OFFSET ${offset}
        LIMIT ${limit}
      """

    def queryUntilNoResult(limit: Int, offset: Int): List[String] = {
      EventLogger.raise(Event(execPartialDBpdeiaCompanyQuery,s"Execute partial company query (limit: $limit, offset: $offset)"))

      val query = QueryFactory.create(queryString(limit, offset));
      val companyQueryExec = QueryExecutionFactory.sparqlService(dbPediaSparqlEndpointUrlString, query)
      val companyResourceSet = companyQueryExec.execSelect()

      val companiesResourceUris = collection.mutable.ListBuffer.empty[String]
      companyResourceSet.hasNext match {
        case true => {
          while(companyResourceSet.hasNext) {
            val companyResource = companyResourceSet.next()
            val companyResourceUri = companyResource.get("x").toString
            companiesResourceUris += companyResourceUri
          }
          // recursively continue
          companiesResourceUris.toList ++ queryUntilNoResult(limit, offset + limit)
        }
        case false => List.empty[String]
      }
    }

    // start the execution
    queryUntilNoResult(limit = 10000, offset = 0)
  }
}