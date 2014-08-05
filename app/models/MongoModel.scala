package models

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.Play.current
import scala.concurrent.Future
import reactivemongo.core.commands.LastError
import reactivemongo.core.errors.DatabaseException
import play.api.Logger

/**
 * Created by Norman on 31.03.14.
 */
class MongoModel {

  /** Returns the current instance of the driver. */
  def driver = ReactiveMongoPlugin.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection = ReactiveMongoPlugin.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db = ReactiveMongoPlugin.db

}
