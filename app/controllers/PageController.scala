package controllers

import java.io._
import java.util
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.slf4j.LoggerFactory
import reactivemongo.bson.BSONObjectID

import scala.concurrent.duration._
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import actors._
import models.{Event, TextRevision, ExtractionRun, Revision}
import models.Util._
import akka.pattern.{ask, pipe}
import scala.concurrent.{Await, Future}
import play.api.Logger
import akka.util.Timeout
import actors.events.EventLogger
import models.EventTypes._
import scala.Some
import extraction.download.{WikipediaRevisionSelector, WikipediaClient}
import java.net.{URLEncoder, URLDecoder}
import actors.DefaultActors._
import scala.Some
import ch.weisenburger.uima.types.distantsupervision.skala.{Sample, SampleCandidate}
import ch.weisenburger.deprecated_ner


import scala.util.{Failure, Success}

/**
 * Created by Norman on 20.03.14.
 */
object PageController extends Controller {

  val log = LoggerFactory.getLogger(getClass)

  def listDownloadedData = Action.async {
    Revision.getAllPages.map(res => Ok(Json.toJson(res)))
  }

  def listDownloadedDataOfExtractionRun(extractionRunId: String) = Action.async {
    val resources = ExtractionRun.getById(extractionRunId).map(_.get.getResources)
    val pageNames = Revision.getAllPageNames.map(_.map(name => (name, name)).toMap)
    val result = for {
      res <- resources
      names <- pageNames
    } yield {
      for {
        r <- res
        hasData = names.get(r._1).isDefined
      } yield Json.obj("pageTitle" -> r._1, "resource" -> r._2, "hasDownloadedData" -> hasData)
    }

    result.map(result => Ok(Json.toJson(result)))

  }

  def getPageRevsAsJson(pageTitleInUri: String) = Action.async {
    Revision.getPageRevsAsJsonByDecodedResourceName(pageTitleInUri)
      .map(revs => Ok(Json.prettyPrint(Json.toJson(revs))))
  }

  def getTextPageRevsAsJson(pageTitleInUri: String) = Action.async {
    TextRevision.getPageRevsAsJson(pageTitleInUri)
      .map(revs => Ok(Json.prettyPrint(Json.toJson(revs))))
  }

  def downloadResourcesOfExtractionRun(extractionRunId: String) = Action {
    import actors.DefaultActors._
    Akka.system.scheduler.scheduleOnce(1000.microsecond) {
      val run = ExtractionRun.getById(extractionRunId).map(_.get)
      run.onSuccess { case run =>
        val sampleSize = run.getResources.size
        run.getResources.view.zipWithIndex.foreach { case (pageUri, index) => pageDownloader ! DownloadAndSaveArticle(Some(run.id), index + 1, sampleSize, pageUri._1)}
      }
    }
    Ok
  }


  def convertResourcesOfExtractionRunFromWikiMarkupToText(extractionRunId: String) = Action {
    import actors.DefaultActors._
    Akka.system.scheduler.scheduleOnce(1000.microsecond) {
      val run = ExtractionRun.getById(extractionRunId).map(_.get)
      run.onSuccess { case run =>
        val sampleSize = run.getResources.size
        run.getResources.view.zipWithIndex.foreach { case (pageUri, index) => wikiMarkupToTextConverter ! ConvertRevsFromWikiMarkupToText(Some(run.id), index + 1, sampleSize, pageUri._1)}
      }
    }
    Ok
  }

  def extractSamplesFromWikiText(extractionRunId: String) = Action {
    implicit val _exid = Some(new BSONObjectID(extractionRunId))
    import actors.DefaultActors._
    implicit val timeout = new Timeout(1 hour)
    log.info("start sample Extraction request")
    Akka.system.scheduler.scheduleOnce(1 second) {
      val run = Await.result(ExtractionRun.getById(extractionRunId), 10 seconds).get
      log.info("Loaded Run")

      val openFiles = Future.sequence(Seq(
        sampleSaver ? OpenExtractionRun(extractionRunId),
        sampleCandidateSaver ? OpenExtractionRun(extractionRunId)))
      Await.result(openFiles, 5 seconds)

      log.info("Extracting samples")
      val sampleSize = run.getResources.size
      val sampleFinderResults = Future.sequence(
        run.getResources.view.zipWithIndex
          .map { case (pageUri, index) =>
          sampleFinder ? ExtractSamplesFromRevisionTexts(Some(run.id), index + 1, sampleSize, pageUri._1)
        })

      Await.result(sampleFinderResults, 48 hours)

      val closeFiles = Future.sequence(Seq(
        sampleSaver ? CloseExtractionRun(extractionRunId),
        sampleCandidateSaver ? CloseExtractionRun(extractionRunId)))
      Await.result(closeFiles, 5 seconds)

      log.info("finished sample Extraction")
      EventLogger raise Event(finishedSampeExtraction)
    }
    Ok
  }

  def getSamples(extractionRunId: String) = {
    val samplesFilePath = s"${extractionRunId}_samples.zip"
    zip(new File(s"data/samples/$extractionRunId"), new File("data/" +samplesFilePath))
    Application.dataFile(samplesFilePath)
  }


  def updateDownloadedData = Action {
    import actors.DefaultActors._
    Akka.system.scheduler.scheduleOnce(1000.microsecond) {
      val pageNames = Revision.getAllPageNames

      pageNames.onSuccess {
        case pageNames =>
          val size = pageNames.size
          pageNames.view.zipWithIndex.foreach {
            case (pageUri, index) => pageDownloader ! DownloadAndSaveArticle(None, index + 1, size, getLastUriComponent(pageUri))
          }
      }
      pageNames.onFailure {
        case ex => throw ex
      }
    }

    Ok
  }

  def downloadSingleWikiPage(pageTitleInUri: String) = Action {
    //    import models.RevisionJsonConverter._
    //    val revisions = WikipediaRevisionSelector.getRevisionsForExtraction(pageTitleInUri,
    //      WikipediaRevisionSelector.revisionsAtQuartilesPlusLatestForEachYear)
    //    val wikipediaArticleName = URLDecoder.decode(pageTitleInUri, "UTF-8")
    //    val result = WikipediaClient.downloadRevisionContents(revisions, wikipediaArticleName, pageTitleInUri)
    //    Ok(Json.prettyPrint(Json.toJson(result)))
    pageDownloader ! DownloadAndSaveArticle(None, 1, 1, pageTitleInUri)
    Ok("")
  }


  private def zip(directory: File, zipfile: File) = {
    val base = directory.toURI
    var queue: util.Deque[File] = new util.LinkedList[File]
    queue.push(directory)
    val out = new FileOutputStream(zipfile)
    val zout = new ZipOutputStream(out)
    val res = zout
    try {
      while (!queue.isEmpty) {
        val directory = queue.pop()
        for {kid <- directory.listFiles} {
          var name = base.relativize(kid.toURI).getPath
          if (kid.isDirectory) {
            queue.push(kid)
            name = name.endsWith("/") match {
              case true => name
              case false => name + "/"
            }
            zout.putNextEntry(new ZipEntry(name));
          } else {
            zout.putNextEntry(new ZipEntry(name));
            copy(kid, zout);
            zout.closeEntry();
          }
        }
      }
    } finally {
      res.close();
    }
  }

  private def copy(file: File, out: OutputStream ) {
    val in = new FileInputStream(file);
    try {
      copy(in, out);
    } finally {
      in.close();
    }
  }

  import scala.util.control.Breaks.{break, breakable}
  private def copy(in: InputStream, out: OutputStream) {
    val buffer = new Array[Byte](1024);
    breakable { while (true) {
      val readCount = in.read(buffer);
      if (readCount < 0) {
        break
      }
      out.write(buffer, 0, readCount);
    } }
  }


}
