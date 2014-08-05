package extraction.formatters

import java.net.URLEncoder

import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.destinations.{Quad, WriterDestination}
import java.io.FileWriter
import com.hp.hpl.jena.rdf.model.Statement
import java.util.UUID
import extraction.OntologyUtil

/**
 * Created by Norman on 02.05.14.
 */
class SingletonPropertyTurtleSaver(val path: String)  {
  var destination: WriterDestination = initDestination

  val toSingletonProperty = (q: models.Quad, wikipediaArticleName: String) => {
    val singletonProperty = s"${q.predicate}#${UUID.randomUUID}"
    val mainQuad = new Quad(getLanguage(q), "", q.subject, singletonProperty, q.obj, "", getDatatype(q))

    val singletonPropertyOf = new Quad(getLanguage(q), "", singletonProperty, "http://weisenburger.ch/singletonPropertyOf", q.predicate, "", null)

    val contextQuads = q.context.map { case (s, s2) =>
      val (predicateUri, datatype, value) = s match {
        case "fromDate" => ("http://weisenburger.ch/fromDate", "xsd:String", s2)
        case "toDate" => ("http://weisenburger.ch/toDate", "xsd:String", s2)
        case "sourceRevision" => ("http://www.w3.org/ns/prov#wasDerivedFrom", null, s"http://en.wikipedia.org/wiki/${URLEncoder.encode(wikipediaArticleName,"UTF-8")}?oldid=$s2")
      }
      new Quad(getLanguage(q), "", singletonProperty, predicateUri, value, "", datatype)
    }
    List(mainQuad, singletonPropertyOf) ++ contextQuads
  }

  val toPlainQuad = (q: models.Quad) => {
    List(new Quad(getLanguage(q), "", q.subject, q.predicate, q.obj, "", getDatatype(q)))
  }



  private def initDestination = {
    val formatter = new TerseFormatter(quads = false, turtle = true)
    formatter.header
    val writer = new FileWriter(path)

    destination = new WriterDestination(() => writer, formatter)
    destination.open
    destination
  }

  def write(quads: Seq[models.Quad], wikipediaArticleName: String) = {
    val convQuads = quads.flatMap { q => OntologyUtil.isOntologyPredicate(q.predicate) match {
      case true => toPlainQuad(q)
      case false => toSingletonProperty(q, wikipediaArticleName)
    }}
    destination.write(convQuads)
  }

  def close = {
    destination.close
  }

  private def getDatatype(q: models.Quad) = q.datatype.getOrElse("") match {
    case "" => null
    case typeStr => typeStr
  }

  private val extractLanguageFromWikiString = "^wiki=[a-z]*,locale=([a-z]*)$".r
  private def getLanguage(quad: models.Quad) = {
    quad.language match {
      case Some(extractLanguageFromWikiString(extractedLang)) => extractedLang
      case Some(plainLang) => plainLang
      case None => null
    }
  }
}
