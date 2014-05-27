package extraction.formatters

import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.destinations.{Quad, WriterDestination}
import java.io.FileWriter
import com.hp.hpl.jena.rdf.model.Statement
import java.util.UUID
import extraction.OntologyUtil

/**
 * Created by Norman on 02.05.14.
 */
object TurtleSaver {


  val toSingletonProperty = (q: models.Quad) => {
    val singletonProperty = s"${q.predicate}#${UUID.randomUUID}"
    val mainQuad = new Quad(q.language.getOrElse(null), "", q.subject, singletonProperty, q.obj, "", getDatatype(q))

    val singletonPropertyOf = new Quad(q.language.getOrElse(null), "", singletonProperty, "http://weisenburger.ch/singletonPropertyOf", q.predicate, "", null)

    val contextQuads = q.context.map { case (s, s2) =>
      val (predicateUri, datatype) = s match {
        case "fromDate" => ("http://weisenburger.ch/fromDate", "xsd:String")
        case "toDate" => ("http://weisenburger.ch/toDate", "xsd:String")
        case "sourceRevision" => ("http://weisenburger.ch/sourceRevision", "xsd:String")
      }
      new Quad(q.language.getOrElse(null), "", singletonProperty, predicateUri, s2, "", datatype)
    }
    List(mainQuad, singletonPropertyOf) ++ contextQuads
  }

  val toPlainQuad = (q: models.Quad) => {
    List(new Quad(q.language.getOrElse(null), "", q.subject, q.predicate, q.obj, "", getDatatype(q)))
  }



  def save(path: String, quads: Seq[models.Quad]) = {

    val formatter = new TerseFormatter(quads = false, turtle = true)
    formatter.header
    val writer = new FileWriter(path)

    val destination = new WriterDestination(() => writer, formatter)

    val convQuads = quads.flatMap { q => OntologyUtil.isOntologyPredicate(q.predicate) match {
      case true => toPlainQuad(q)
      case false => toSingletonProperty(q)
    }}

      destination.open
      destination.write(convQuads)
      destination.close
    }

  private def getDatatype(q: models.Quad) = q.datatype.getOrElse("") match {
    case "" => null
    case typeStr => typeStr
  }
}
