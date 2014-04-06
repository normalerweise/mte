package models

import play.api.libs.json.{Reads, JsPath, Writes}
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

  implicit val mongoReads: Reads[Page] = (
      (JsPath \ "_id").read[Long] and
      (JsPath \ "title").read[String] and
      (JsPath \ "uriTitle").read[String]
    )(Page.apply _)
}