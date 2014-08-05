package models

import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.Enumeratee
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.bson.BSONObjectID



object EventTypes extends Enumeration {
  type EventType = EventTypeValue
  case class EventTypeValue(name: String, description: String) extends Val(name)
  val execPartialDBpdeiaCompanyQuery = EventTypeValue("execPartialDBpdeiaCompanyQuery","Executing Partial DBpedia Company Query")
  val queriedDBpdeiaCompanies = EventTypeValue("queriedDBpdeiaCompanies","Finished Querying Companies from DBpedia")
  val generatedSample = EventTypeValue("generatedSample", "Generated Sample")
  val downloadedPageRevisions = EventTypeValue("downloadedPageRevisions", "Downloaded Page Revisions")
  val droppedRevisions = EventTypeValue("droppedRevisions", "Dropped Revisions")
  val wikipageDoesNoExist = EventTypeValue("wikipageDoesNoExist", "Wiki page does not exist")
  val exception = EventTypeValue("exception", "Exception Occured")
}
import EventTypes._


case class Event(_id: BSONObjectID, typ: EventType, timestamp: DateTime, caption: String, details: Option[JsValue])


object Event extends MongoModel {

  import EventJsonConverter._

  private def collection: JSONCollection = db.collection[JSONCollection]("events")


  def apply(typ: EventType) =
    new Event(BSONObjectID.generate, typ, new DateTime, typ.description , None)

  def apply(typ: EventType, customDescription: String) =
    new Event(BSONObjectID.generate, typ, new DateTime, customDescription, None)

  def apply(typ: EventType, details: JsValue) =
    new Event(BSONObjectID.generate, typ, new DateTime, typ.description, Some(details))

  def apply(typ: EventType, customDescription: String, details: JsValue) =
    new Event(BSONObjectID.generate, typ, new DateTime, customDescription, Some(details))

  def save(e: Event) = collection.insert(e)

  def listAsJson(n: Int) = collection.find(Json.obj())
    .sort(Json.obj("timestamp" -> -1)).cursor[JsValue].collect[List](n)

}

object EventJsonConverter {

  implicit val eventTypeWrite = new Writes[EventType] {
    def writes(e: EventType) = Json.toJson(e.toString)
  }

  implicit val bsonObjectIdWrite = new Writes[BSONObjectID] {
    def writes(id: BSONObjectID) = Json.toJson(id.stringify)
  }

  implicit val eventWrite: Writes[Event] = (
      (JsPath \ "_id").write[BSONObjectID] and
      (JsPath \ "type").write[EventType] and
      (JsPath \ "timestamp").write[DateTime] and
      (JsPath \ "description").write[String] and
      (JsPath \ "details").writeNullable[JsValue]
    )(unlift(Event.unapply))

  val asJson: Enumeratee[Event,JsValue] = Enumeratee.map[Event]{ e => Json.toJson(e) }
}
