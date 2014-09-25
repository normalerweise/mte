package models

import reactivemongo.bson.BSONObjectID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.GetLastError


object ExtractionRunTypes extends Enumeration {
  type ExtractionRunType = ExtractionRunTypeValue

  case class ExtractionRunTypeValue(name: String, description: String) extends Val(name)

  val infoboxExtraction = ExtractionRunTypeValue("infoboxExtraction", "Infobox Extraction Run")
  val freeTextExtraction = ExtractionRunTypeValue("freeTextExtraction", "Free Text Extraction Run")

  def withNameOpt(str: String):Option[ExtractionRunType] = {
    ExtractionRunTypes.values.find( _.toString == str).asInstanceOf[Option[ExtractionRunType]]
  }

}

import ExtractionRunTypes._


case class ExtractionRun(id: BSONObjectID, typ: ExtractionRunType, description: String, createdOn: DateTime, startedOn: Option[DateTime], finishedOn: Option[DateTime], resources: Option[List[JsValue]]){
  lazy val getResources = resources match {
    case Some(resList) => resList.map { res =>
      val uri = ExtractionRunJsonConverter.uriReads.reads(res).asOpt match {
        case Some(uri) => (Util.getLastUriComponent(uri), uri)
        case None => throw new Exception("unable to parse resources");("","")
      }
      uri
    }
    case None => List.empty[(String,String)]
  }
  def copyWithResources(resources: List[String]) =
    this.copy(resources = Some(resources.map( resource => Json.obj("uri" -> resource))))

}

object ExtractionRun extends MongoModel {

  import ExtractionRunJsonConverter._
  import models.BSONObjectIdJsonConverter._

  private def collection: JSONCollection = db.collection[JSONCollection]("extraction_runs")

  def applyAll(id: BSONObjectID, typ: ExtractionRunType, description: String, createdOn: DateTime, startedOn: Option[DateTime], finishedOn: Option[DateTime], resources: Option[List[JsValue]]) =
    new ExtractionRun(id, typ, description, createdOn, startedOn, finishedOn, resources)

  def apply(typ: ExtractionRunType, description: String) =
    new ExtractionRun(BSONObjectID.generate, typ, description, new DateTime, None, None, None)


  def newInfoboxExtractionRun(description: String) = ExtractionRun(infoboxExtraction, description)


  def save(e: ExtractionRun) = collection.save(e)

  def getById(id: String) =
    collection.find(Json.obj("_id" -> id))
      .cursor[ExtractionRun].headOption

  def getByIdAsJson(id: String) =
    collection.find(Json.obj("_id" -> id))
      .cursor[JsValue].headOption

  val projection = Json.obj("resources" -> 0)
  def listAsJsonWoSamples = collection.find(Json.obj(), projection)
    .sort(Json.obj("createdOn" -> -1)).cursor[JsValue].collect[List]()

  def delete(extractionRunId: String) = {
    val selector = Json.obj("_id" -> extractionRunId)
    collection.remove(selector, GetLastError(), false)
  }
}


object ExtractionRunJsonConverter {

  import models.BSONObjectIdJsonConverter._

  val uriReads = (JsPath \ "uri").read[String]

  implicit val extractionRunTypeWrite = new Writes[ExtractionRunType] {
    def writes(t: ExtractionRunType) = Json.toJson(t.toString)
  }

  implicit val extractionRunTypeRead = new Reads[ExtractionRunType] {
    override def reads(js: JsValue): JsResult[ExtractionRunType] = js.asOpt[String] match {
      case Some(str) => ExtractionRunTypes.withNameOpt(str) match {
        case Some(eventType) =>  JsSuccess(eventType)
        case None => JsError("no event type known for " + str)
      }
      case None => JsError("unable to read event type")
    }
  }

  implicit val mongoReads: Reads[ExtractionRun] = (
    (__ \ "_id").read[BSONObjectID] and
      (__ \ "type").read[ExtractionRunType] and
      (__ \ "description").read[String] and
      (__ \ "createdOn").read[DateTime](DateTimeJsonConverter.isoDateimeReads) and
      (__ \ "startedOn").readNullable[DateTime](DateTimeJsonConverter.isoDateimeReads) and
      (__ \ "finishedOn").readNullable[DateTime] and
      (__ \ "resources").readNullable[List[JsValue]]
    )(ExtractionRun.applyAll _)

  implicit val mongoWrites: Writes[ExtractionRun] = (
    (__ \ "_id").write[BSONObjectID] and
      (__ \ "type").write[ExtractionRunType] and
      (__ \ "description").write[String] and
      (__ \ "createdOn").write[DateTime](DateTimeJsonConverter.isoDateimeWrites) and
      (__ \ "startedOn").writeNullable[DateTime](DateTimeJsonConverter.isoDateimeWrites) and
      (__ \ "finishedOn").writeNullable[DateTime] and
      (__ \ "resources").writeNullable[List[JsValue]]
    )(unlift(ExtractionRun.unapply _))

}
