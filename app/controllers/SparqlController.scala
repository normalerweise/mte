package controllers

import scala.concurrent.duration._
import akka.pattern.ask
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import actors.events.EventLogger
import actors.{QueriedCompanies, QueryCompanies}
import models.Event
import models.EventTypes._


/**
 * Created by Norman on 19.03.14.
 */
object SparqlController extends Controller {

  import actors.DefaultActors._

  def queryCompaniesFromDBpedia = Action {
    Akka.system.scheduler.scheduleOnce(100 microseconds) {
      dbPedia ? QueryCompanies onSuccess {
        case result: QueriedCompanies =>
          Helper.writeCompanyResourcesFile(result.companyUris)
          EventLogger raise Event(queriedDBpdeiaCompanies)(None)
      }
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
