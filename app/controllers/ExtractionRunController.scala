package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import models.{ExtractionRunPageResult, ExtractionRun}
import play.api.libs.json._
import models.ExtractionRunJsonConverter._
import scala.util.{Failure, Success}
import scala.concurrent.Future

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

  def delete(id: String) = Action.async {
    val deletePageResultsFuture = ExtractionRunPageResult.deleteAllResultsOf(id)
    val deleteRunFuture = ExtractionRun.delete(id)
    val result = Future.sequence(Seq(deletePageResultsFuture, deleteRunFuture))

    result.onComplete {
      case Failure(e) => throw e
      case Success(lasterror) => {
        println("successfully removed extraction results of run " + id + " " + lasterror)
      }
    }

    result.map { lastErrors =>
      val results = lastErrors.map { lastError => lastError.ok match {
      case true => Ok
      case false => InternalServerError(lastError.code + " " + lastError.err)
    } }.filterNot( _ == Ok)
      results.length match {
        case x if x <= 0 => Ok
        case _ => results.head
      }
    }
  }

}
