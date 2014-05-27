package controllers

import scala.concurrent.duration._
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import actors.{ExtractSamplesFromRevisionTexts, ConvertRevsFromWikiMarkupToText, DownloadAndSaveArticle}
import models.{Event, TextRevision, ExtractionRun, Revision}
import models.Util._
import akka.pattern.{ ask, pipe }
import scala.concurrent.{Await, Future}
import ner.LingPipeTrainingGenerator
import play.api.Logger
import akka.util.Timeout
import actors.events.EventLogger
import models.EventTypes._
import actors.ConvertRevsFromWikiMarkupToText
import scala.Some
import actors.DownloadAndSaveArticle
import actors.ExtractSamplesFromRevisionTexts
import extraction.download.{WikipediaRevisionSelector, WikipediaClient}

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

  def getTextPageRevsAsJson(pageTitleInUri: String) = Action.async {
    TextRevision.getPageRevsAsJson(pageTitleInUri)
      .map( revs => Ok(Json.prettyPrint(Json.toJson(revs))))
  }

   def downloadResourcesOfExtractionRun(extractionRunId: String) = Action {
     import actors.DefaultActors._
     Akka.system.scheduler.scheduleOnce(1000.microsecond) {
       val run = ExtractionRun.getById(extractionRunId).map( _.get)
       run.onSuccess{ case run =>
         val sampleSize = run.getResources.size
         run.getResources.view.zipWithIndex.foreach{ case (pageUri,index) => pageDownloader ! DownloadAndSaveArticle(Some(run.id), index+1, sampleSize, pageUri._1)}
       }
     }
     Ok
   }


  def convertResourcesOfExtractionRunFromWikiMarkupToText(extractionRunId: String) = Action {
    import actors.DefaultActors._
    Akka.system.scheduler.scheduleOnce(1000.microsecond) {
      val run = ExtractionRun.getById(extractionRunId).map( _.get)
      run.onSuccess{ case run =>
        val sampleSize = run.getResources.size
        run.getResources.view.zipWithIndex.foreach{ case (pageUri,index) => wikiMarkupToTextConverter ! ConvertRevsFromWikiMarkupToText(Some(run.id), index+1, sampleSize, pageUri._1)}
      }
    }
    Ok
  }

  def extractSamplesFromWikiText(extractionRunId: String) = Action {
    import actors.DefaultActors._
    Akka.system.scheduler.scheduleOnce(1000.microsecond) {
      // TODO: acutally predicates don't need to be passed to the extractSamples... message
      val predicates = List(
        "http://dbpedia.org/ontology/revenue",
        "http://dbpedia.org/ontology/assets",
        "http://dbpedia.org/ontology/equity",
        "http://dbpedia.org/ontology/operatingIncome",
        "http://dbpedia.org/ontology/numberOfEmployees",
        "http://dbpedia.org/ontology/netIncome"
      )
      LingPipeTrainingGenerator.init(predicates)
      Logger.info("init of training generator finished")
      val run = ExtractionRun.getById(extractionRunId).map( _.get)
      run.onSuccess{ case run =>
        implicit val timeout = Timeout(24 hours)
        val sampleSize = run.getResources.size

//        val samples = (sampleFinder ? ExtractSamplesFromRevisionTexts(Some(run.id), 1, sampleSize, run.getResources.head._1,predicates)).mapTo[List[ner.Sample]]
//        Await.result(samples, 100 seconds)
////        Logger.info("samples done")
////        val samples = Future.sequence(run.getResources.map( pageUri => (sampleFinder ? ConvertRevsFromWikiMarkupToText(Some(run.id), 1, sampleSize, pageUri._1)).mapTo[List[ner.Sample]]))
////        val ret = Await.result(samples, 7200 seconds)
        val samples = Future.sequence( run.getResources.view.zipWithIndex.map{ case (pageUri,index) => (sampleFinder ? ExtractSamplesFromRevisionTexts(Some(run.id), index+1, sampleSize, pageUri._1, predicates)).mapTo[List[ner.Sample]]})
        val ret = Await.result(samples.map(_.flatten), 24 hours)
        val distinctRet = ret.toList.distinct
        ner.Util.saveSamplesAsXML(distinctRet, s"data/${extractionRunId}_sample.xml")
        Logger.info("finished sample Extraction")
        Logger.info("ret size: " + ret.size)
        Logger.info("distinct ret size: " + distinctRet.size)

        implicit val exId = Some(run.id)
        EventLogger raise Event(finishedSampeExtraction)

      }
    }
    Ok
  }

  def getSamples(extractionRunId: String) = Application.dataFile(s"${extractionRunId}_sample.xml")



  def updateDownloadedData = Action {
    import actors.DefaultActors._
    Akka.system.scheduler.scheduleOnce(1000.microsecond) {
      val pageNames = Revision.getAllPageNames

      pageNames.onSuccess {
        case pageNames =>
          val size = pageNames.size
          pageNames.view.zipWithIndex.foreach {
            case (pageUri, index) => pageDownloader ! DownloadAndSaveArticle(None,index+1, size, getLastUriComponent(pageUri)) }
      }
      pageNames.onFailure {
        case ex => throw ex
      }
    }

    Ok
  }

  def downloadSingleWikiPage(pageTitleInUri: String) = Action {
    val revisions = WikipediaRevisionSelector.getRevisionsForExtraction(pageTitleInUri,
      WikipediaRevisionSelector.revisionsAtQuartilesPlusLatestForEachYear)
    val result = WikipediaClient.downloadRevisionContents(revisions,pageTitleInUri)
    Ok(result.mkString("\n"))
  }

 }
