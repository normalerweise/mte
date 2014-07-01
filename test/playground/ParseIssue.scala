package playground

import play.api.libs.json.{JsPath, Reads, Json}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.io.Source
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

/**
 * Created by Norman on 03.04.14.
 */
object ParseIssue extends App {

  case class Revision(id: Long, timestamp: DateTime, downloadedOn: DateTime, language: String = "en", content: Option[String])

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


  implicit val mongoReads:  Reads[Revision] = (
    (JsPath \ "_id").read[Long] and
      (JsPath \ "timestamp").read[DateTime](isoDateimeReads) and
      (JsPath \ "downloadedOn").read[DateTime](isoDateimeReads) and
      (JsPath \ "language").read[String] and
      (JsPath \ "content").readNullable[String]
    )(Revision)




  val string = Source.fromURL("http://localhost:9000/api/v1/pages/De_Beers/revs").getLines().mkString("")

  val jsval = Json.parse(string)
  val revision = jsval.validate[List[Revision]] match {
    case succ : JsSuccess[List[Revision]] => println("sicces")
    case err : JsError => println(err)
  }


  println(revision)

}
