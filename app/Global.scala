
import actors.events.EventLogger
import models.Event
import models.EventTypes._
import org.slf4j.bridge.SLF4JBridgeHandler
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.Results._

import org.dbpedia.extraction.dataparser.UnitValueParser

/**
 * Created by Norman on 31.03.14.
 */
object Global extends GlobalSettings {

  override def onStart(app: Application) = {
    // Optionally remove existing handlers attached to j.u.l root logger
    //SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    //SLF4JBridgeHandler.install();
    Logger.info("installed jul bridge")

    java.util.logging.Logger.getLogger("test").fine("Jul bridge is working" + classOf[UnitValueParser].getName)

    java.util.logging.Logger.getLogger(classOf[UnitValueParser].getName).fine("Test parser logger")

  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful {
      val exceptionJson = EventLogger.raiseExceptionEvent(ex)
      InternalServerError(exceptionJson)
    }
  }

}
