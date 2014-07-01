package actors.events

import akka.actor.{Actor, Props}
import org.slf4j.LoggerFactory
import play.api.libs.iteratee.{Enumerator, Concurrent}
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import akka.pattern.ask
import play.api.libs.json._

import play.api.Play.current
import models.Event
import models.EventJsonConverter._
import models.EventTypes._
import reactivemongo.bson.BSONObjectID


case class Connect()
case class Connected(enumerator: Enumerator[Event])


class EventLogger extends Actor {

  val (enumerator, channel) = Concurrent.broadcast[Event]

  def receive = {
    case Connect => sender ! Connected(enumerator)
    case e: Event => process(e)
  }

  def process(e: Event) = {
    Event.save(e)
    notifyConnectedUsers(e)
  }

  def notifyConnectedUsers(e: Event) = {
    //Logger.info("sentEvent: " + e)
    channel.push(e)
  }
}


object EventLogger {
  val default = Akka.system.actorOf(Props[EventLogger])
  implicit val timeout = Timeout(1 second)

  val log = LoggerFactory.getLogger(getClass)

  def raise(e: Event) = {
    default ! e
  }

  def raiseExceptionEvent(ex: Throwable)(implicit extractionRunId: Option[BSONObjectID] = None) = {
    val exceptionJson = Json.obj(
      "type" -> ex.getClass.getCanonicalName,
      "message" -> ex.getMessage
    )
    log.error("Exception Event occured",ex)
    raise(Event(exception, ex.getClass.getCanonicalName +": " + ex.getMessage, exceptionJson))
    exceptionJson
  }

  def listenAsJson = {
    default ? Connect map {
      case Connected(eventEnumerator) => eventEnumerator &> asJson
    }
  }
}