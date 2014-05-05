package playground

import org.dbpedia.extraction.destinations.formatters.{TerseFormatter, TerseBuilder}
import org.dbpedia.extraction.destinations.{Quad, WriterDestination}
import java.io.FileWriter
import org.dbpedia.extraction.mappings.PageContext
import com.hp.hpl.jena.rdf.model.{Statement, ModelFactory}
import com.hp.hpl.jena.graph.NodeFactory
import com.hp.hpl.jena.datatypes.{BaseDatatype, RDFDatatype}
import java.util.UUID

/**
 * Created by Norman on 02.05.14.
 */
object TurtleSaver extends App {

  save("data/rdf.tt", Seq(models.Quad("http://en.dbpedia.org/resource/Samsung", "http://dbpedia.org/ontology/product", "asd",Some("xsd:String"), Some("en"), Map("fromDate" -> "2013")))
  )

  def save(path: String,quads: Seq[models.Quad]) = {

  val formatter = new TerseFormatter( quads = false, turtle = true )
  formatter.header
  val writer = new FileWriter(path)

  val destination = new WriterDestination(()=> writer, formatter)

  val convQuads = quads.flatMap { q =>
    val singletonProperty = s"${q.predicate}#${UUID.randomUUID}"
    val mainQuad = new Quad(q.language.getOrElse(null), "", q.subject, singletonProperty,q.obj, "", q.datatype.getOrElse(null))
    val singletonPropertyOf = new Quad(q.language.getOrElse(null), "", singletonProperty, "http://weisenburger.ch/singletonPropertyOf", q.predicate, "", null)

    val contextQuads = q.context.map { case (s,s2) =>
      val (predicateUri, datatype) = s match {
        case "fromDate" => ("http://weisenburger.ch/fromDate", "xsd:String")
        case "toDate" => ("http://weisenburger.ch/toDate", "xsd:String")
        case "sourceRevision" => ("http://weisenburger.ch/sourceRevision", "xsd:String")
      }
      new Quad(q.language.getOrElse(null), "", singletonProperty, predicateUri, s2, "", datatype)
    }
    List(mainQuad,singletonPropertyOf) ++ contextQuads
  }

  destination.open
  destination.write(convQuads)
  destination.close
  }
}
