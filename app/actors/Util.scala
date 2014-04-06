package actors

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

}
