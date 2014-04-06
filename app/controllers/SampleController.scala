package controllers

import scala.util.{Success, Failure, Random}
import scala.concurrent.duration._
import scala.concurrent.future
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json

import models.{Event, ExtractionRun}
import models.EventTypes._
import actors.events.EventLogger
import models.ExtractionRunJsonConverter._
import play.api.Logger
import scala.io.Source

object SampleController extends Controller {

  def generateSample(size: Int, extractionRunId: String) = Action {
      Akka.system.scheduler.scheduleOnce(100 microseconds) {
        val extractionRun = ExtractionRun.getById(extractionRunId)

        val sampleResources = future {
          val companies = Helper.readCompanyResourceUris
          val sample = size match {
            case size if size <= 0 => companies
            case size if size < companies.size => Random.shuffle(companies).take(size)
            case size if size >= companies.size => companies
          }
          sample
        }

        val savedRunSample = for {
          runOption <- extractionRun
          run = runOption.get if runOption.isDefined
          resources <- sampleResources
          updatedRun = run.copyWithResources(resources)
          saveResult <- ExtractionRun.save(updatedRun)
        } yield updatedRun

        savedRunSample.onComplete {
          case Success(updatedRun) => EventLogger raise Event(generatedSample)(Some(updatedRun.id))
          case Failure(t) => EventLogger.raiseExceptionEvent(t)
        }
      }
      Ok
  }

  def generateSampleFromFile(extractionRunId: String) = Action { request =>
    Akka.system.scheduler.scheduleOnce(100 microseconds) {
      val multipartBody = request.body.asMultipartFormData.get;
      val extractionRunId = multipartBody.dataParts.get("extractionRunId").get.mkString("")

      val extractionRun = ExtractionRun.getById(extractionRunId)

      val sampleResources = future {
        val fileRef = multipartBody.files.head.ref
        val resources = Source.fromFile(fileRef.file).getLines().toList
        resources
      }

      val savedRunSample = for {
        runOption <- extractionRun
        run = runOption.get if runOption.isDefined
        resources <- sampleResources
        updatedRun = run.copyWithResources(resources)
        saveResult <- ExtractionRun.save(updatedRun)
      } yield updatedRun

      savedRunSample.onComplete {
        case Success(updatedRun) => EventLogger raise Event(generatedSample)(Some(updatedRun.id))
        case Failure(t) => EventLogger.raiseExceptionEvent(t)
      }
    }
    Ok
  }

  def getSampleFromFile = Action.async {
    val futureCompanies = scala.concurrent.Future {
      Helper.readRandomSample
    }
    futureCompanies.map(companies => Ok(Json.toJson(companies)))
  }

}
