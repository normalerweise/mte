package actors

import java.lang.ref.WeakReference
import java.net.{URLEncoder, URLDecoder}

/**
 * Created by Norman on 03.04.14.
 */
object Util {

   def measure[T](f: => T) = {
    val start = System.currentTimeMillis
    val result = f
    val end = System.currentTimeMillis
    (result, end - start)
  }
  
  def decodeResourceName(dbPediaResourceName: String) = {
    URLDecoder.decode(dbPediaResourceName.replace("+", "%2B"), "UTF-8").replace("%2B", "+")
  }

  def encodeWikiArticleName(wikiArticleName: String) = {
    URLEncoder.encode(wikiArticleName, "UTF-8")
  }

  def gc() = {
    var obj = new Object()
    var ref = new WeakReference[Object](obj)
    obj = null
    while(ref.get != null) {
      System.gc
    }
  }

  def buildWikipediaURL(artilcleName: String, revision: Long) = s"http://en.wikipedia.org/w/index.php?title=$artilcleName&oldid=$revision"

}
