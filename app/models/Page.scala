package models

import play.api.libs.json.{Reads, JsPath, Writes}
import org.joda.time.DateTime
import play.api.libs.functional.syntax._

/**
 * Created by Norman on 01.04.14.
 */
case class Page(id: Long, title: String, dbpediaResourceName: String, dbpediaResourceNameDecoded: String, wikipediaArticleName: String)

object Page {

}

object PageJsonConverter {

  implicit val mongoWrites: Writes[Page] = (
    (JsPath \ "_id").write[Long] and
    (JsPath \ "title").write[String] and
    (JsPath \ "dbpediaResourceName").write[String] and
    (JsPath \ "dbpediaResourceNameDecoded").write[String] and
    (JsPath \ "wikipediaArticleName").write[String]
  )(unlift(Page.unapply))

  implicit val mongoReads: Reads[Page] = (
      (JsPath \ "_id").read[Long] and
      (JsPath \ "title").read[String] and
      (JsPath \ "dbpediaResourceName").read[String] and
      (JsPath \ "dbpediaResourceNameDecoded").read[String] and
      (JsPath \ "wikipediaArticleName").read[String]
    )(Page.apply _)
}