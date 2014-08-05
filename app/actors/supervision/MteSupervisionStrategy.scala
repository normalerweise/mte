package actors.supervision

import scala.concurrent.duration._
import akka.actor._
import akka.actor.SupervisorStrategy._
import actors.events.EventLogger
import akka.actor.OneForOneStrategy
import play.api.Logger
import extractors.QueryResultParserException
import models.RevisionException


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
      case ex: QueryResultParserException =>
        EventLogger.raiseExceptionEvent(ex)
        Restart
      case ex: RevisionException =>
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
    case p: Props => sender ! context.actorOf(p)
  }
}
