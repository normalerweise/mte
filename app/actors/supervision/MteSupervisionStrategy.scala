package actors.supervision

import scala.concurrent.duration._
import akka.actor._
import akka.actor.SupervisorStrategy._
import actors.events.EventLogger
import akka.actor.OneForOneStrategy
import play.api.Logger
import models.RevisionException
import java.io.IOException
import extraction.download.WikipediaClient.ResultParsingException


/**
 * Created by Norman on 31.03.14.
 */
class MteSupervisionStrategy extends SupervisorStrategyConfigurator {
  override def create(): SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case ex: ArithmeticException =>
        EventLogger.raiseExceptionEvent(ex)
        Resume
      case ex: NullPointerException =>
        EventLogger.raiseExceptionEvent(ex)
        Restart
      case ex: IllegalArgumentException =>
        EventLogger.raiseExceptionEvent(ex)
        Stop
      case ex: ResultParsingException =>
        EventLogger.raiseExceptionEvent(ex)
        Restart
      case ex: RevisionException =>
        EventLogger.raiseExceptionEvent(ex)
        Restart
      case ex: IOException =>
        EventLogger.raiseExceptionEvent(ex)
        Restart
      case ex: org.sweble.wikitext.engine.CompilerException =>
        EventLogger.raiseExceptionEvent(ex)
        Restart
      case ex: reactivemongo.core.errors.GenericDriverException =>
        EventLogger.raiseExceptionEvent(ex)
        Restart
      case ex: java.util.concurrent.TimeoutException =>
        EventLogger.raiseExceptionEvent(ex)
        Restart
      case ex: Exception =>
        EventLogger.raiseExceptionEvent(ex)
        Escalate
    }
}

class MteSupervisor extends Actor {

  override val supervisorStrategy: SupervisorStrategy  = new MteSupervisionStrategy().create

  def receive = {
    case CreateActor(p, name) => sender ! context.actorOf(p, name)
  }
}

case class CreateActor(p:Props, name: String)
