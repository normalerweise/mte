package models

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import play.api.libs.json.JsSuccess
import reactivemongo.bson.BSONObjectID


object Util {

   def getLastUriComponent(uri: String) = {
    val title = uri.split('/').last
    assert(title != null )
    title
  }


}



object BSONObjectIdJsonConverter {

  implicit val bsonObjectIdWrite = new Writes[BSONObjectID] {
    def writes(id: BSONObjectID) = Json.toJson(id.stringify)
  }

  implicit val bsonObjectIdRead = new Reads[BSONObjectID] {
    override def reads(js: JsValue): JsResult[BSONObjectID] = js.asOpt[String] match {
      case Some(str) => JsSuccess(new BSONObjectID(str))
      case None => JsError("unable to read DateTime")
    }
  }

}


object DateTimeJsonConverter {

  implicit val isoDateimeReads = new Reads[DateTime] {
    val isoFmt = ISODateTimeFormat.dateTime();
    override def reads(js: JsValue): JsResult[DateTime] = js.asOpt[String] match {
      case Some(str) => JsSuccess(isoFmt.parseDateTime(str))
      case None => js.asOpt[Long] match {
        case Some(long)=> JsSuccess(new DateTime(long))
        case none => JsError("unable to read DateTime")
      }
    }
  }

  implicit val isoDateimeWrites = new Writes[DateTime] {
    val isoFmt = ISODateTimeFormat.dateTime();
    override def writes(dt: DateTime): JsValue = Json.toJson(isoFmt.print(dt))
  }


}
