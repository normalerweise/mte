package controllers

import play.api.mvc.{Action, Controller}
import play.api.Play.current
import play.api.libs.json.Json
import scala.util.Random

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import models.Event
import models.EventTypes._
import actors.events.EventLogger
import play.api.libs.concurrent.Akka


/**
 * Created by Norman on 20.03.14.
 */
object SampleController extends Controller {

  def generateSample(size: Int) = Action { request =>

    Akka.system.scheduler.scheduleOnce(100 microseconds) {
      val companies = Helper.readCompanyResourceUris

      val randomCompanies = size match {
        case size if size <= 0 => companies
        case size if size < companies.size => Random.shuffle(companies).take(size)
        case size if size >= companies.size => companies
      }

      Helper.writeRandomSampleFile(randomCompanies)
      EventLogger.raise(Event(generatedSample))
    }
    Ok
  }

  def getSampleFromFile = Action.async {
    val futureCompanies = scala.concurrent.Future {
      Helper.readRandomSample
    }
    futureCompanies.map(companies => Ok(Json.toJson(companies)))
  }

}
