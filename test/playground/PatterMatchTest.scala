package playground

/**
 * Created by Norman on 08.04.14.
 */
object PatterMatchTest extends App {


  val strOpt: Option[String] = Some("wiki=en,locale=en")
  val redirectString = "#REDIRECT [[Rolls-Royce Holdings]]\n{{R from move}}"

  val redirectPattern = """#REDIRECT \[\[(.*)\]\]\s*.*""".r
  val extractLanguageFromWikiString = "^wiki=[a-z]*,locale=([a-z]*)$".r

//  val res  = strOpt match {
//      case Some(extractLanguageFromWikiString(extractedLang)) => extractedLang
//      case Some(plainLang) => plainLang
//      case None => null
//    }

  val res = redirectString match {
    case redirectPattern(redArt) => redArt
    case _ => "fuu"
  }


  println(res)

}
