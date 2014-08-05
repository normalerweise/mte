
import actors.events.EventLogger
import models.Event
import models.EventTypes._
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.Future
import play.api.mvc.Results._

/**
 * Created by Norman on 31.03.14.
 */
object Global extends GlobalSettings {

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful {
      val exceptionJson = EventLogger.raiseExceptionEvent(ex)
      InternalServerError(exceptionJson)
    }
  }

}
