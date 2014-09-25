package models

import java.util.regex.Pattern

import play.modules.reactivemongo.json.collection.JSONCollection

/**
 * Created by Norman on 08.05.14.
 */
object TextRevision extends TRevision {

  protected def collection: JSONCollection = db.collection[JSONCollection]("text_revisions")

  def getPageRevsWithPreprocessing(pageTitleInUri: String) = {
    getPageRevs(pageTitleInUri).map(revs =>
      revs.map( rev => rev.copy(content = Some(preprocessContent(rev.content.get)))))

  }

  val preprocessPattern = "Š\n|Š|\n".r.pattern
  private def preprocessContent(str: String) = {
    preprocessPattern.matcher(str).replaceAll(" ")
  }

}
