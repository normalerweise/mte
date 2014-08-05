package models

import play.api.libs.json.{JsPath, Writes}
import org.joda.time.DateTime
import play.api.libs.functional.syntax._

/**
 * Created by Norman on 01.04.14.
 */
case class Page(id: Long, title: String, uriTitle: String)

object Page {

}

object PageJsonConverter {

  implicit val mongoWrites: Writes[Page] = (
    (JsPath \ "_id").write[Long] and
    (JsPath \ "title").write[String] and
    (JsPath \ "uriTitle").write[String]
  )(unlift(Page.unapply))
}