package models

import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Enumerator
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.core.commands._
import reactivemongo.bson.{BSONDocument, BSONValue}
import play.modules.reactivemongo.json.BSONFormats
import play.api.libs.json.JsSuccess
import reactivemongo.api.indexes.Index
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.Some
import reactivemongo.core.commands.GroupField
import play.api.libs.json.JsObject
import models.RevisionException

/**
 * Created by Norman on 01.04.14.
 */
case class RevisionException(message: String) extends Exception(message)

case class Revision(id: Long, timestamp: DateTime, downloadedOn: DateTime, language: String = "en", page: Option[Page], content: Option[String])


object Revision extends MongoModel {

  private def collection: JSONCollection = db.collection[JSONCollection]("revisions")

  collection.indexesManager.ensure(Index(Seq(("page.uriTitle", Ascending)), Some("pageUriTitle"),false,true,false,false))

  import RevisionJsonConverter._

  def apply(id: Long, timestamp: DateTime) =
    new Revision(id, timestamp, new DateTime, "en", None, None)

  def apply(id: Long, timestamp: DateTime, content: String) =
    new Revision(id, timestamp, new DateTime, "en", None, Some(content))

  def withoutContentFromWikiJson(js: JsObject) =
    js.validate[Revision](wikiJsonSparseDataReads).get

  val contentPath = (JsPath \ "*" )
  def withContentFromWikiJsonAndPage(js: JsObject, page: Page) =
    js.validate[Revision](wikiJsonwithContentReads) match {
      case JsSuccess(revision,_) => Some(revision.copy(page = Some(page)))
      case JsError( (contentPath, _) :: tail) => None
      case JsError(err) => throw new RevisionException(page.title + " unable to parse Revision. Err: " + err + " ; from: " + Json.prettyPrint(js))
    }


  private def getTimestamp(n: JsValue) = {
    dateFromUTCTimestamp((n \ "timestamp").as[String])
  }

   def dateFromUTCTimestamp(wikiTimestamp: String) = {
    val wikiRevTimestampFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    wikiRevTimestampFormatter.parseDateTime(wikiTimestamp)
  }

  def saveBulk(revisions: Seq[Revision]) = {
    val enum = Enumerator.enumerate(revisions)
    revisions.map( r => collection.save(r)(defaultContext,mongoWrites))
  }

  def getPageRevsAsJson(pageTitleInUri: String) =
    collection.find(Json.obj("page.uriTitle" -> pageTitleInUri))
      .cursor[JsValue].collect[List]()

  def getAllPages = {
    val gropu = GroupField("page.uriTitle")(
      ("revisions", AddToSet("$_id")),
      ("numberOfRevisions", SumValue(1))
    )
    val command = Aggregate("revisions", Seq(
      gropu
   ))

    db.command(command).map(res => res.map( bdoc => BSONFormats.toJSON(bdoc) ))
  }

  def stats = collection.stats()

}

object RevisionJsonConverter {

  implicit val wikiTimestampReads = new Reads[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] =
      json.asOpt[String] match {
        case Some(str) => JsSuccess(Revision.dateFromUTCTimestamp(str))
        case None => JsError("Unable to parse timestamp as String")
      }
  }

  implicit val wikiJsonSparseDataReads: Reads[Revision] = (
    (JsPath \ "revid").read[Long] and
      (JsPath \ "timestamp").read[DateTime](wikiTimestampReads)
    )(Revision.apply(_,_))


  implicit val wikiJsonwithContentReads: Reads[Revision] = (
    (JsPath \ "revid").read[Long] and
      (JsPath \ "timestamp").read[DateTime](wikiTimestampReads) and
      (JsPath \ "*").read[String]
    )(Revision.apply(_, _, _))

  implicit val mongoWrites:  Writes[Revision] = (
      (JsPath \ "_id").write[Long] and
      (JsPath \ "timestamp").write[DateTime] and
      (JsPath \ "downloadedOn").write[DateTime] and
      (JsPath \ "language").write[String] and
      (JsPath \ "page").writeNullable[Page](PageJsonConverter.mongoWrites) and
      (JsPath \ "content").writeNullable[String]
    )(unlift(Revision.unapply))

}