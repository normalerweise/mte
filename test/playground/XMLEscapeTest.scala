package playground

import java.util.regex.Pattern

/**
 * Created by Norman on 01.07.14.
 */
object XMLEscapeTest extends App {

  val str = "Per H. Utnegaard, President & CEO"

  val res = escapePredefinedXMLCharacters(str)

  println(str)
  println(res)



  private def escapePredefinedXMLCharacters(str: String) = {
    // result will be an XML -> escape predefined XML symbols
    val sb = new StringBuilder()
    for(i <- 0 until str.length) {
      str.charAt(i) match {
        case '"' => sb.append("&quot;")
        case '\'' => sb.append("&apos;")
        case '<' => sb.append("&lt;")
        case '>' => sb.append("&gt;")
        case '&' => sb.append("&amp;")
        case c => sb.append(c)
      }
    }
    sb.toString
  }



}
