package controllers

import scala.concurrent.duration._
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import play.api.libs.json._

import extractors.RelevantRevisionDownloader
import actors.DownloadAndSavePage
import models.{ExtractionRun, Revision}
import models.Util._

/**
  * Created by Norman on 20.03.14.
  */
object PageController extends Controller {

   def listDownloadedData = Action.async {
     Revision.getAllPages.map(res => Ok(Json.toJson(res)))
   }

  def listDownloadedDataOfExtractionRun(extractionRunId: String) = Action.async {
    val resources = ExtractionRun.getById(extractionRunId).map( _.get.getResources)
    val pageNames = Revision.getAllPageNames.map( _.map(name => (name,name) ).toMap)
    val result = for {
      res <- resources
      names <- pageNames
    } yield {
      for{
        r <- res
        hasData = names.get(r._1).isDefined
      } yield Json.obj( "pageTitle" -> r._1, "resource" -> r._2, "hasDownloadedData" -> hasData )
    }

    result.map( result => Ok(Json.toJson(result)))

  }

  def getPageRevsAsJson(pageTitleInUri: String) = Action.async {
    Revision.getPageRevsAsJson(pageTitleInUri)
      .map( revs => Ok(Json.prettyPrint(Json.toJson(revs))))
  }

   def downloadResourcesOfExtractionRun(extractionRunId: String) = Action {
     import actors.DefaultActors._
     Akka.system.scheduler.scheduleOnce(1000.microsecond) {
       val run = ExtractionRun.getById(extractionRunId).map( _.get)
       run.onSuccess{ case run =>
         val sampleSize = run.getResources.size
         run.getResources.view.zipWithIndex.foreach{ case (pageUri,index) => pageDownloader ! DownloadAndSavePage(Some(run.id), index+1, sampleSize, pageUri._1)}
       }
     }
     Ok
   }

  def updateDownloadedData = Action {
    import actors.DefaultActors._
    Akka.system.scheduler.scheduleOnce(1000.microsecond) {
      val pageNames = Revision.getAllPageNames

      pageNames.onSuccess {
        case pageNames =>
          val size = pageNames.size
          pageNames.view.zipWithIndex.foreach {
            case (pageUri, index) => pageDownloader ! DownloadAndSavePage(None,index+1, size, getLastUriComponent(pageUri)) }
      }
      pageNames.onFailure {
        case ex => throw ex
      }
    }

    Ok
  }

  def downloadSingleWikiPage(pageTitleInUri: String) = Action {
    val result = RelevantRevisionDownloader.download(pageTitleInUri)
    Ok(result.toString)
  }

 }
