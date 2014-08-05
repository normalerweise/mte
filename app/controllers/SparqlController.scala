package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Codec
import akka.pattern.ask
import play.api.mvc.{Action, Controller}
import play.api.libs.json.Json
import actors.events.EventLogger
import actors.{QueriedCompanies, QueryCompanies}
import models.Event
import models.EventTypes._



/**
 * Created by Norman on 19.03.14.
 */
object SparqlController extends Controller {
  import actors.DefaultActors._
  implicit val codec = Codec.UTF8

  def queryCompaniesFromDBpedia = Action {
    dbPedia ? QueryCompanies onSuccess { case result: QueriedCompanies =>
        Helper.writeCompanyResourcesFile(result.companyUris)
        EventLogger raise Event(queriedDBpdeiaCompanies)
      }
    Ok
  }



  def getCompanieUrisFromFile = Action.async {
    val futureCompanies = scala.concurrent.Future {
      Helper.readCompanyResourceUris
    }
    futureCompanies.map(companies => Ok(Json.toJson(companies)))
  }


}
