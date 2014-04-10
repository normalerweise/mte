package actors


import akka.actor.Actor
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}
import models.Event
import models.EventTypes._
import actors.events.EventLogger
import controllers.FileUtil
import play.api.libs.json.Json

abstract class Query {
  val id: String
  val queryStringWithLimitAndOffset: (Int, Int) => String
}

case class AdhocQuery(queryStringWithLimitAndOffset: (Int, Int) => String) extends Query {
  val id = "adhoc"
}

case class PredefinedQuery(
                            id: String,
                            queryStringWithLimitAndOffset: (Int, Int) => String
                            ) extends Query

case class QueryResult(uris: List[String])


class DBpediaSparqlEndpointActor extends Actor {

  val dbPediaSparqlEndpointUrlString = "http://dbpedia.org/sparql"


  def receive = {
    case query: AdhocQuery =>
      sender ! QueryResult(execute(query))

    case query: PredefinedQuery =>
      val result = execute(query)
      cacheResult(result, query.id)
      logSuccess(query)
  }


  private def execute(q: Query) = {

    def queryUntilNoResult(limit: Int, offset: Int): List[String] = {
      EventLogger raise Event(executePartialSparqlQuery, s"Execute partial query of ${q.id} (limit: $limit, offset: $offset)",
        Json.obj("queryId" -> q.id, "query" -> q.toString))(None)

      val query = QueryFactory.create(q.queryStringWithLimitAndOffset(limit, offset));
      val dbPediaSparqlServiceForQuery = QueryExecutionFactory.sparqlService(dbPediaSparqlEndpointUrlString, query)
      val resultSet = dbPediaSparqlServiceForQuery.execSelect()

      val resultResourceUris = collection.mutable.ListBuffer.empty[String]
      resultSet.hasNext match {
        case true => {
          while (resultSet.hasNext) {
            val element = resultSet.next()
            val resourceURI = element.get("x").toString
            resultResourceUris += resourceURI
          }
          // recursively continue
          resultResourceUris.toList ++ queryUntilNoResult(limit, offset + limit)
        }
        case false => List.empty[String]
      }
    }

    // start the execution
    queryUntilNoResult(limit = 10000, offset = 0)
  }


  private def cacheResult(result: List[String], queryId: String) = {
    import FileUtil._
    writeOneElementPerLine(cacheFilePathForQueryId(queryId), result)
  }

  private def logSuccess(query: Query) = {
    EventLogger raise Event(executedSparqlQuery, s"Executed SPARQL Query ${query.id}",
      Json.obj("queryId" -> query.id, "query" -> query.queryStringWithLimitAndOffset(0,0) ))(None)
  }


}