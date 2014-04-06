package models

import org.joda.time.DateTime
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.Enumeratee
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands._
import play.api.libs.json.JsSuccess
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.Some
import reactivemongo.core.commands.SumValue
import models.EventTypes.EventTypeValue
import reactivemongo.core.commands.GroupField
import reactivemongo.core.commands.AddToSet
import play.modules.reactivemongo.json.BSONFormats


object EventTypes extends Enumeration {
  type EventType = EventTypeValue
  case class EventTypeValue(name: String, description: String) extends Val(name)
  val execPartialDBpdeiaCompanyQuery = EventTypeValue("execPartialDBpdeiaCompanyQuery","Executing Partial DBpedia Company Query")
  val queriedDBpdeiaCompanies = EventTypeValue("queriedDBpdeiaCompanies","Finished Querying Companies from DBpedia")
  val generatedSample = EventTypeValue("generatedSample", "Generated Sample")
  val updatedExtractionRun = EventTypeValue("updatedExtractionRun", "Updated Extraction Run")
  val downloadedPageRevisions = EventTypeValue("downloadedPageRevisions", "Downloaded Page Revisions")
  val extractedPageRevisions = EventTypeValue("extractedPageRevisions", "Extracted Page Revisions")
  val droppedRevisions = EventTypeValue("droppedRevisions", "Dropped Revisions")
  val wikipageDoesNoExist = EventTypeValue("wikipageDoesNoExist", "Wiki page does not exist")
  val initializedInfoboxExtractor = EventTypeValue("initializedInfoboxExtractor", "Initialized Infobox extractor")
  val stoppedInfoboxExtractor = EventTypeValue("stoppedInfoboxExtractor", "Stopped Infobox extractor")
  val noRevisionDataFound = EventTypeValue("noRevisionDataFound", "No Revision Data found")
  val exception = EventTypeValue("exception", "Exception Occured")

  def withNameOpt(str: String):Option[EventType] = {
    EventTypes.values.find( _.toString == str).asInstanceOf[Option[EventType]]
  }

}
import EventTypes._


case class Event(id: BSONObjectID, extractionRunId: Option[BSONObjectID], typ: EventType, timestamp: DateTime, caption: String, details: Option[JsValue])


object Event extends MongoModel {

  import EventJsonConverter._

  private def collection: JSONCollection = db.collection[JSONCollection]("events")


  def apply(typ: EventType)(implicit extractionRunId: Option[BSONObjectID]) =
    new Event(BSONObjectID.generate, extractionRunId, typ, new DateTime, typ.description , None)

  def apply(typ: EventType, customDescription: String)(implicit extractionRunId: Option[BSONObjectID] = None) =
    new Event(BSONObjectID.generate, extractionRunId, typ, new DateTime, customDescription, None)

  def apply(typ: EventType, details: JsValue)(implicit extractionRunId: Option[BSONObjectID]) =
    new Event(BSONObjectID.generate, extractionRunId, typ, new DateTime, typ.description, Some(details))

  def apply(typ: EventType, customDescription: String, details: JsValue)(implicit extractionRunId: Option[BSONObjectID]) =
    new Event(BSONObjectID.generate, extractionRunId, typ, new DateTime, customDescription, Some(details))

  def save(e: Event) = collection.insert(e)

  def listAsJson(n: Int) = collection.find(Json.obj())
    .sort(Json.obj("timestamp" -> -1)).cursor[JsValue].collect[List](n)

  def eventStatsForRun(extractionRunId: String) = {
    val matchh = Match(BSONDocument("extractionRunId" -> extractionRunId ))
    val gropu = GroupField("type")(
    //  ("revisions", AddToSet("$_id")),
      ("numberOfOccurences", SumValue(1))
    )
    val command = Aggregate("events", Seq( matchh, gropu ))

    db.command(command).map(res => res.map( bdoc => BSONFormats.toJSON(bdoc) ))
  }

}

object EventJsonConverter {

  import BSONObjectIdJsonConverter._

  implicit val eventTypeWrite = new Writes[EventType] {
    def writes(e: EventType) = Json.toJson(e.toString)
  }

  implicit val eventTypeRead = new Reads[EventType] {
    override def reads(js: JsValue): JsResult[EventType] = js.asOpt[String] match {
      case Some(str) => EventTypes.withNameOpt(str) match {
        case Some(eventType) =>  JsSuccess(eventType)
        case None => JsError("no event type known for " + str)
      }
      case None => JsError("unable to read event type")
    }
  }


  implicit val eventWrite: Writes[Event] = (
      (JsPath \ "_id").write[BSONObjectID] and
      (JsPath \ "extractionRunId").writeNullable[BSONObjectID] and
      (JsPath \ "type").write[EventType] and
      (JsPath \ "timestamp").write[DateTime] and
      (JsPath \ "description").write[String] and
      (JsPath \ "details").writeNullable[JsValue]
    )(unlift(Event.unapply))

  val asJson: Enumeratee[Event,JsValue] = Enumeratee.map[Event]{ e => Json.toJson(e) }(play.api.libs.concurrent.Execution.Implicits.defaultContext)
}
