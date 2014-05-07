package models

import play.modules.reactivemongo.json.collection.JSONCollection

/**
 * Created by Norman on 08.05.14.
 */
object TextRevision extends TRevision {

  protected def collection: JSONCollection = db.collection[JSONCollection]("text_revisions")

}
