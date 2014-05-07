package models

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.core.commands._
import play.api.libs.json._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import play.modules.reactivemongo.json.BSONFormats
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.util.Language
import play.api.libs.json.JsSuccess
import reactivemongo.api.indexes.Index
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.Some
import reactivemongo.core.commands.SumValue
import reactivemongo.core.commands.GroupField
import play.api.libs.json.JsObject
import reactivemongo.core.commands.AddToSet
import scala.util.{Success, Failure}
import play.api.Logger
import scala.concurrent.Future

/**
 * Created by Norman on 01.04.14.
 */
case class RevisionException(message: String) extends Exception(message)
case class WikiDBpediaPropertiesRequirePageInformationException() extends Exception

case class Revision(id: Long, timestamp: DateTime, downloadedOn: DateTime, language: String = "en", page: Option[Page], content: Option[String]) {

  lazy val subjectURI = page match {
    case Some(page) => wikiTitle.language.resourceUri.append(wikiTitle.decodedWithNamespace)
    case None => throw new WikiDBpediaPropertiesRequirePageInformationException
  }

  lazy val wikiLanguage = Language(language)

  lazy val wikiTitle = page match {
    case Some(page) => WikiTitle.parse(page.uriTitle, wikiLanguage)
    case None => throw new WikiDBpediaPropertiesRequirePageInformationException
  }

  lazy val dbPediaTitle = page match {
    case Some(page) => page.uriTitle
    case None => throw new WikiDBpediaPropertiesRequirePageInformationException
  }

}

// Revisions with WIKI Markup as content -> Used for Infobox extraction
abstract trait TRevision extends MongoModel {

  protected def collection: JSONCollection

  collection.indexesManager.ensure(Index(Seq(("page.uriTitle", Ascending)), Some("pageUriTitle"),false,true,false,false))

  import RevisionJsonConverter._

  def applyAll(id: Long, timestamp: DateTime, downloadedOn: DateTime, language: String = "en", page: Option[Page], content: Option[String]) =
    new Revision(id, timestamp, downloadedOn, language, page, content)

  def apply(id: Long, timestamp: DateTime) =
    new Revision(id, timestamp, new DateTime, "en", None, None)

  def apply(id: Long, timestamp: DateTime, content: String) =
    new Revision(id, timestamp, new DateTime, "en", None, Some(content))

  def apply(id: Long, timestamp: DateTime, page: Page, content: String) =
    new Revision(id, timestamp, new DateTime, "en", Some(page), Some(content))


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
    Future.sequence(revisions.map{ r => collection.save(r)(executionContext,mongoWrites) })
  }

  def getPageRevsAsJson(pageTitleInUri: String) =
    collection.find(Json.obj("page.uriTitle" -> pageTitleInUri))
      .cursor[JsValue].collect[List]()

  def getPageRevs(pageTitleInUri: String) =
    collection.find(Json.obj("page.uriTitle" -> pageTitleInUri))
      .cursor[Revision](mongoReads,executionContext).collect[List]()


  def getAllPagesTupled = getAllPages.mapTo[Stream[(String,Int)]]

  def getAllPages = {
    _getAllPages.map(res => res.map( bdoc => BSONFormats.toJSON(bdoc) ))
  }

  def deleteAllRevisionsOf(articleNameInURL: String) = {
    val selector = Json.obj("page.uriTitle" -> articleNameInURL)
    collection.remove(selector, GetLastError(), false)
  }

  private def _getAllPages = {
    val gropu = GroupField("page.uriTitle")(
      //     ("revisions", AddToSet("$_id")),
      ("numberOfRevisions", SumValue(1))
    )
    val command = Aggregate("revisions", Seq( gropu ))

    db.command(command)
  }

  case class FaliedToExtractPageNameException() extends Exception
  def getAllPageNames =
    getAllPages.map( pages => pages.map { page =>
      (JsPath \ "_id").read[String].reads(page).asOpt match {
        case Some(pageTitleInUri) => pageTitleInUri
        case None => throw new FaliedToExtractPageNameException
      }
    })

  def stats = collection.stats()

  def drop = collection.drop()

}

object Revision extends TRevision {

  protected def collection: JSONCollection = db.collection[JSONCollection]("revisions")

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
      (JsPath \ "timestamp").write[DateTime](DateTimeJsonConverter.isoDateimeWrites) and
      (JsPath \ "downloadedOn").write[DateTime](DateTimeJsonConverter.isoDateimeWrites)  and
      (JsPath \ "language").write[String] and
      (JsPath \ "page").writeNullable[Page](PageJsonConverter.mongoWrites) and
      (JsPath \ "content").writeNullable[String]
    )(unlift(Revision.unapply _))

  implicit val mongoReads:  Reads[Revision] = (
      (JsPath \ "_id").read[Long] and
      (JsPath \ "timestamp").read[DateTime](DateTimeJsonConverter.isoDateimeReads) and
      (JsPath \ "downloadedOn").read[DateTime](DateTimeJsonConverter.isoDateimeReads)  and
      (JsPath \ "language").read[String] and
      (JsPath \ "page").readNullable[Page](PageJsonConverter.mongoReads) and
      (JsPath \ "content").readNullable[String]
    )(Revision.applyAll _ )

}