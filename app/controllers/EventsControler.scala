package controllers

import play.api.mvc.{Action, Controller}
import models.Event
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by Norman on 02.04.14.
 */
object EventsController extends Controller {

  def getLastNEvents(n: Int) = Action.async {
    Event.listAsJson(n).map( jsList => Ok(Json.prettyPrint(Json.toJson(jsList))))
  }

}
