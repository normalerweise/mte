package playground

/**
 * Created by Norman on 08.04.14.
 */
object PatterMatchTest extends App {

  val toDate  = Option(null) match {
    case Some(toDate) => toDate
    case None => "Null case"
  }

  println(toDate)

}
