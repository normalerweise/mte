package models

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.Play.current
import reactivemongo.api.MongoDriver


/**
 * Created by Norman on 31.03.14.
 */
class MongoModel {

  /** Returns the current instance of the driver. */
  def driver: MongoDriver = ReactiveMongoPlugin.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection = ReactiveMongoPlugin.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db = ReactiveMongoPlugin.db

  implicit val executionContext = driver.system.dispatcher

}
