package controllers

import java.io.File
import scala.io.Codec
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.{Action, Controller}
import play.api.libs.json.JsValue
import play.api.{Play, Logger}
import play.api.Play.current
import play.api.libs.iteratee.Enumerator
import play.api.libs.EventSource
import play.api.libs.EventSource.EventIdExtractor
import actors.events.EventLogger



object Application extends Controller {
  implicit val codec = Codec.UTF8

  def index = Action {
    Ok(views.html.Index())
  }

  def dataFile(path: String) = Action {
    val fullPath = "data/" + path
    Logger.info("serve: " + fullPath)
    sendFile(fullPath)
  }

  def logFile(path: String) = Action {
    val fullPath = "logs/" + path
    Logger.info("serve: " + fullPath)
    streamDynamicallySizedTextFile(fullPath)
  }

  // so far it is better to handle the event type on my own
  //implicit val eventNameExtractor = EventNameExtractor[JsValue]( e => (e \ "type").asOpt[String])
  implicit val eventIdExtractor = EventIdExtractor[JsValue](e => (e \ "_id").asOpt[String])

  def connectEventsSSE = Action.async {
    EventLogger.listenAsJson map {
      jsonEnumerator =>
        Ok.feed(jsonEnumerator &> EventSource()).as("text/event-stream")
    }
  }

  private def sendFile(fullPath: String) = {
    Logger.info("App: " + Play.application.path)
    Ok.sendFile(
      content = new java.io.File(fullPath),
      inline = true
    )
  }

  private def streamDynamicallySizedTextFile(fullPath: String) = {
    val dataContent: Enumerator[Array[Byte]] = Enumerator.fromFile(new File(fullPath))
    Ok.chunked(dataContent).withHeaders(("Content-Type", "text/plain"), ("Content-Disposition", "inline"))
  }

  def config = Action {
    Ok(Play.current.configuration.entrySet.toList.sortBy(_._1).mkString("\n"))
  }

}
