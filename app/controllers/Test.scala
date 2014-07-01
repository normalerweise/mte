package controllers

import ch.weisenburger.deprecated_ner.SentenceSplitter
import ch.weisenburger.uima.FinancialDataSamplePipelineFactory
import ch.weisenburger.uima.annotator.MyStanfordPOSTaggerWrapper
import ch.weisenburger.uima.util.UimaTypesSugar
import de.unihd.dbs.uima.annotator.stanfordtagger.StanfordPOSTaggerWrapper
import de.unihd.dbs.uima.types.heideltime
import models.TextRevision
import org.apache.uima.cas.FSIterator
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.{JsValue, JsArray, JsObject, Json}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.JavaConversions._
import scala.concurrent.Await

/**
 * Created by Norman on 29.03.14.
 */
object Test extends Controller with MongoController with UimaTypesSugar {

  private def collection: JSONCollection = db.collection[JSONCollection]("persons")

//  def list = Action.async {
//    val res = models.Revision.listAsJson
//    res.map( result => Ok(Json.toJson(result)))
//  }

  def create(name: String) = Action.async {
    val json = Json.obj(
      "name" -> name,
      "age" -> 21,
      "created" -> new java.util.Date().getTime())

    collection.insert(json).map(lastError =>
      Ok("Mongo LastError: %s".format(lastError)))
  }


  val testPipeline = FinancialDataSamplePipelineFactory.
    createTestPipeline(Seq(new MyStanfordPOSTaggerWrapper))


  def split(pageTitleInUri: String) = Action {
    import scala.concurrent.duration._

    val revisions = Await.result(TextRevision.getPageRevs(pageTitleInUri), 10 seconds)
    val sentences = revisions.filter(_.id == 486641062).flatMap { rev =>
     val jCas = testPipeline.process(rev.content.get,"")
     val iter = jCas.getAnnotationIndex(heideltime.Sentence.`type`).iterator()
     val ss = (for(ss <- iter; s = ss.asInstanceOf[heideltime.Sentence] ) yield { s"${s.getEnd},${ s.getBegin}<" + s.getCoveredText + ">" }).toList
      println(rev.id + ": " + ss.size)
      ss
    }
    Ok(sentences.mkString("\n\n"))
  }

}
