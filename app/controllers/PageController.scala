package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import extractors.RelevantRevisionDownloader
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import java.io.File
import play.api.Play.current
import scala.io.Codec
import actors.DownloadAndSavePage
import models.Revision

/**
  * Created by Norman on 20.03.14.
  */
object PageController extends Controller {

  implicit val codec = Codec.UTF8

   def listDownloadedData = Action.async {
     Revision.getAllPages.map(res => Ok(Json.toJson(res)))
   }

  def getPageRevsAsJson(pageTitleInUri: String) = Action.async {
    Revision.getPageRevsAsJson(pageTitleInUri)
      .map( revs => Ok(Json.prettyPrint(Json.toJson(revs))))
  }

   def downloadSample = Action {
     import actors.DefaultActors._
     Akka.system.scheduler.scheduleOnce(1000.microsecond) {
       val sample = Helper.readRandomSample
       sample.foreach( pageUri => pageDownloader ! DownloadAndSavePage(getPageTitle(pageUri)))
     }
     Ok
   }

  def getPageTitle(pageUri: String) = {
   val title = pageUri.split('/').last
    assert(title != null )
    title
  }

  def downloadSingleWikiPage(pageTitleInUri: String) = Action {
    val result = RelevantRevisionDownloader.download(pageTitleInUri)
    Ok(result.toString)
  }

 }
