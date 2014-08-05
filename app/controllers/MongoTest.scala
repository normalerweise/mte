package controllers

import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.{JsValue, JsArray, JsObject, Json}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created by Norman on 29.03.14.
 */
object MongoTest extends Controller with MongoController {

  private def collection: JSONCollection = db.collection[JSONCollection]("persons")

//  def list = Action.async {
//    val res = models.Revision.listAsJson
//    res.map( result => Ok(Json.toJson(result)))
//  }

  def create(name: String) = Action.async {
    val json = Json.obj(
      "name" -> name,
      "age" -> 21,
      "created" -> new java.util.Date().getTime())

    collection.insert(json).map(lastError =>
      Ok("Mongo LastError: %s".format(lastError)))
  }

}
