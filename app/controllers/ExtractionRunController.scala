package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import models.ExtractionRun
import play.api.libs.json._
import models.ExtractionRunJsonConverter._

object ExtractionRunController extends Controller {

  def listAsJson = Action.async {
    ExtractionRun.listAsJson.map( runs => Ok(Json.toJson(runs)))
  }

  def create = Action(parse.json) { request =>
    request.body.validate((JsPath \ "description").read[String]).map {
      case description =>
        val run = ExtractionRun.newInfoboxExtractionRun(description)
        ExtractionRun.save(run)
        Ok(Json.toJson(run))
    }.recoverTotal{
      e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
    }
  }

  def getById(id: String) = Action.async {
    ExtractionRun.getByIdAsJson(id).map( _ match {
      case Some(run) => Ok(run)
      case None => BadRequest
    })
  }

}
