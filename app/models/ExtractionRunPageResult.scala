package models

import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.core.commands._
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.api.indexes.Index
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.GroupField
import scala.Some
import reactivemongo.core.commands.AddToSet
import models.Quad

//import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import reactivemongo.api.indexes.Index
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.Some

/**
 * Created by Norman on 03.04.14.
 */

case class Quad(subject: String, predicate: String, obj: String, context: Map[String, String])

case class ExtractionRunPageResult(id: String, extractionRunId: BSONObjectID, extractionRunDescription: String, page: Page, quads: List[Quad])

object ExtractionRunPageResult extends MongoModel {

  private def collection: JSONCollection = db.collection[JSONCollection]("extraction_run_page_results")

  collection.indexesManager.ensure(Index(Seq(("page.uriTitle", Ascending)), Some("pageUriTitle"), false, true, false, false))

  def applyAll(id: String, extractionRunId: BSONObjectID, extractionRunDescription: String, page: Page, quads: List[Quad]) =
    new ExtractionRunPageResult(id, extractionRunId, extractionRunDescription, page, quads)

  def apply(extractionRunId: BSONObjectID, extractionRunDescription: String, page: Page, quads: List[Quad]) =
    new ExtractionRunPageResult(s"${extractionRunId.stringify}:${page.uriTitle}", extractionRunId, extractionRunDescription, page, quads)

  import ExtractionRunPageResultJsonConverter._

  def save(runPageResult: ExtractionRunPageResult) =
    collection.save(runPageResult)(executionContext, mongoWritesExtended)

  def getPageQuadsAsJson(pageTitleInUri: String) =
    collection.find(Json.obj("page.uriTitle" -> pageTitleInUri))
      .cursor[JsValue].collect[List]()

  def getPageQuads(pageTitleInUri: String) =
    collection.find(Json.obj("page.uriTitle" -> pageTitleInUri))
      .cursor[ExtractionRunPageResult].collect[List]()

  def getAsJson(extractionRunId: String) =
    collection.find(Json.obj("extractionRunId" -> extractionRunId))
      .cursor[JsValue].collect[Seq]()

  def get(extractionRunId: String) =
    collection.find(Json.obj("extractionRunId" -> extractionRunId))
      .cursor[ExtractionRunPageResult].collect[Seq]()



  def getAllAsJson =
    collection.find(Json.obj())
      .cursor[JsValue].collect[Seq]()



  def getAsJson(extractionRunId: String, pageTitleInUri: String) =
    collection.find(Json.obj("extractionRunId" -> extractionRunId, "page.uriTitle" -> pageTitleInUri))
      .cursor[JsValue].headOption

  def listAsJson(extractionRunId: String) =
    collection.find(Json.obj("extractionRunId" -> extractionRunId), Json.obj("extractionRunId" -> "", "page.uriTitle" -> "", "numberOfQuads" -> "", "numberOfTemporalQuads" -> "" ))
      .cursor[JsValue].collect[Seq]()


//  def listAsJson(extractionRunId: String)  = {
//      val matchh = Match(BSONDocument("extractionRunId" -> extractionRunId ))
//      val gropu = GroupField("page.uriTitle")(
//        ("quads", AddToSet("$numberOfQuads")),
//
//      )
//      val command = Aggregate("extraction_run_page_results", Seq( matchh, gropu ))
//
//      db.command(command).map(res => res.map( bdoc => BSONFormats.toJSON(bdoc)))
//  }
}


object ExtractionRunPageResultJsonConverter {

  import BSONObjectIdJsonConverter._

  implicit val quadMongoReads: Reads[Quad] = (
    (JsPath \ "subject").read[String] and
      (JsPath \ "predicate").read[String] and
      (JsPath \ "object").read[String] and
      (JsPath \ "context").read[Map[String, String]]
    )(Quad.apply _)

  implicit val quadMongoWrites: Writes[Quad] = (
      (JsPath \ "subject").write[String] and
      (JsPath \ "predicate").write[String] and
      (JsPath \ "object").write[String] and
      (JsPath \ "context").write[Map[String, String]]
    )(unlift(Quad.unapply _))

  implicit val mongoReads: Reads[ExtractionRunPageResult] = (
      (JsPath \ "_id").read[String] and
      (JsPath \ "extractionRunId").read[BSONObjectID] and
      (JsPath \ "extractionRunDescription").read[String] and
      (JsPath \ "page").read[Page](PageJsonConverter.mongoReads) and
      (JsPath \ "quads").read[List[Quad]]
    )(ExtractionRunPageResult.applyAll _)

  implicit val mongoWritesExtended = new Writes[ExtractionRunPageResult] {
    override def writes(o: ExtractionRunPageResult): JsValue = Json.obj(
      "_id" -> o.id,
      "extractionRunId" -> o.extractionRunId,
      "extractionRunDescription" -> o.extractionRunDescription,
      "page" -> Json.toJson(o.page)(PageJsonConverter.mongoWrites),
      "numberOfQuads" -> o.quads.size,
      "numberOfTemporalQuads" -> o.quads.filter(_.context.get("fromDate").isDefined).size,
      "quads" -> Json.toJson(o.quads)
    )
  }

  implicit val mongoWrites: Writes[ExtractionRunPageResult] = (
      (JsPath \ "_id").write[String] and
      (JsPath \ "extractionRunId").write[BSONObjectID] and
      (JsPath \ "extractionRunDescription").write[String] and
      (JsPath \ "page").write[Page](PageJsonConverter.mongoWrites) and
      (JsPath \ "quads").write[List[Quad]]
    )(unlift(ExtractionRunPageResult.unapply _))
}
