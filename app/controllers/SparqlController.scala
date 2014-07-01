package controllers

import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import actors.PredefinedQuery


object SparqlController extends Controller {

  import actors.DefaultActors._

  val resourcesOfTypeDBpediaCompany = PredefinedQuery("resources-of-type-dbpedia-company", (limit: Int, offset: Int) =>
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
      """)

  val resourcesOfTypeDBpediaSettlement = PredefinedQuery("resources-of-type-dbpedia-settlement", (limit: Int, offset: Int) =>
      s"""
        PREFIX res: <http://dbpedia.org/ontology/>

        SELECT distinct ?x
        WHERE
          {
            {
              SELECT DISTINCT ?x
              WHERE
                {
                  ?x a res:Settlement
                } ORDER BY ASC(?x)
            }
            FILTER NOT EXISTS
             { ?x res:wikiPageRedirects ?y }
          }
        OFFSET ${offset}
        LIMIT ${limit}
      """)

  val resourcesOfTypeDBpediaAmericanFootballPlayer = PredefinedQuery("resources-of-type-dbpedia-americanfootballplayer", (limit: Int, offset: Int) =>
    s"""
        PREFIX res: <http://dbpedia.org/ontology/>

        SELECT distinct ?x
        WHERE
          {
            {
              SELECT DISTINCT ?x
              WHERE
                {
                  ?x a res:AmericanFootballPlayer
                } ORDER BY ASC(?x)
            }
            FILTER NOT EXISTS
             { ?x res:wikiPageRedirects ?y }
          }
        OFFSET ${offset}
        LIMIT ${limit}
      """)


  val predefinedQueries: Map[String, PredefinedQuery] = Map(
    resourcesOfTypeDBpediaCompany.id -> resourcesOfTypeDBpediaCompany,
    resourcesOfTypeDBpediaSettlement.id -> resourcesOfTypeDBpediaSettlement,
    resourcesOfTypeDBpediaAmericanFootballPlayer.id -> resourcesOfTypeDBpediaAmericanFootballPlayer
  )

  def triggerPredefinedQueryExecution(queryId: String) = Action {
    predefinedQueries.get(queryId) match {
      case Some(predefinedQuery) =>
        dbPedia ! predefinedQuery
        Ok
      case None => queryNotKnownResult(queryId)
    }
  }

  def getCachedResultsForPredefinedQery(queryId: String) = Action.async {
    import FileUtil._

    predefinedQueries.get(queryId) match {
      case Some(predefinedQuery) =>
        val result = future {
          readFileOneElementPerLine(cacheFilePathForQueryId(queryId))
        }
        result.map( result => Ok(Json.toJson(result)))
      case None => future {
        queryNotKnownResult(queryId)
      }
    }
  }

  private def queryNotKnownResult(queryId: String) = BadRequest(s"Query with id '$queryId' is not known")
}
