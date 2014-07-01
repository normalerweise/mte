package actors

import java.net.URLDecoder

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

}
