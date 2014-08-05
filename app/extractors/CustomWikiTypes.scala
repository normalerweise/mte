package extractors

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsObject, JsValue}

//case class PageWithRevisionsContent(title: String, revisions: Seq[Revision])
//
//case class PageWithExtractionResults(title: String, quadruples: Seq[String])
//
//object PageBuilder {
//  def fromPageNodeWithRevisionContent(node: xml.Node) = {
//    val title = (node \\ "@title").text
//    val revisions = (node \\ "revisions" \ "rev").map(
//      rev => RevisionBuilder.fromRevisionNodeWithContent(rev))
//    PageWithRevisionsContent(title, revisions)
//  }
//}
