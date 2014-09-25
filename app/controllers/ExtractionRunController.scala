package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import models.{Event, ExtractionRunPageResult, ExtractionRun}
import play.api.libs.json._
import models.ExtractionRunJsonConverter._
import scala.util.{Failure, Success}
import scala.concurrent.Future
import reactivemongo.core.commands.LastError

object ExtractionRunController extends Controller {

  def listAsJson = Action.async {
    ExtractionRun.listAsJsonWoSamples.map(runs => Ok(Json.toJson(runs)))
  }

  def create = Action(parse.json) { request =>
    request.body.validate((JsPath \ "description").read[String]).map {
      case description =>
        val run = ExtractionRun.newInfoboxExtractionRun(description)
        ExtractionRun.save(run)
        Ok(Json.toJson(run))
    }.recoverTotal {
      e => BadRequest("Detected error:" + JsError.toFlatJson(e))
    }
  }

  def getById(id: String) = Action.async {
    ExtractionRun.getByIdAsJson(id).map(_ match {
      case Some(run) => Ok(run)
      case None => BadRequest
    })
  }

  def delete(id: String) = Action.async {
    val deletePageResultsFuture = ExtractionRunPageResult.deleteAllResultsOf(id)
    val deleteRunFuture = ExtractionRun.delete(id)
    val deleteEventsFuture = Event.deleteEventsOfExtractionRun(id)
    val result = Future.sequence(Seq(deletePageResultsFuture, deleteRunFuture, deleteEventsFuture))

    result.map { _.find(_.inError) match {
        case Some(lastError) => InternalServerError(s"${lastError.message} [${lastError.code}]")
        case None => Ok
      }
    }
  }

}
